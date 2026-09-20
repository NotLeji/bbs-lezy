package bbslod;

import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import org.joml.Matrix4f;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import org.joml.Vector3f;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * The LOD engine: distance shells with a camera-FOV zoom bias, applied around each form's render.
 *
 * <p>Tier 2 hides the form for the frame (via {@code visible}'s runtime value, which BBS's own
 * keyframe system uses, so the two never fight); tier 1 is read by the Phase 4 mixin, which trims
 * deep bones. Registered once from {@link BBSLodClient} — these are plain Fabric events, and they
 * run every frame.</p>
 */
public class LodEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger("bbslod");

    /** The camera fov BBS defaults to (70 degrees, in radians) — the reference the zoom bias divides by. */
    private static final float REFERENCE_FOV = (float) Math.toRadians(70F);

    /** Forms whose {@code visible} runtime value this engine set this frame, so it can put them back. */
    private static final Set<Form> touched = Collections.newSetFromMap(new IdentityHashMap<>());

    private static long frames;
    private static long tier1;
    private static long tier2;

    /** Scratch: the film camera's pose, filled from the film panel's runner. Render-thread only,
     * and consumed within {@link #before} the moment it is read. */
    private static final Camera FILM_CAMERA = new Camera();
    private static final Matrix4f RENDER_VIEW = new Matrix4f();
    private static final Vector3f OFFSET = new Vector3f();
    private static boolean warned;

    public static void register()
    {
        FormRenderEvents.BEFORE.register(LodEngine::before);
        FormRenderEvents.AFTER.register(LodEngine::after);
        FilmEvents.RENDER_AFTER.register(LodEngine::onRenderAfter);
        FilmEvents.SHUTDOWN.register(LodEngine::onShutdown);
    }

    private static void before(Form form, FormRenderingContext context)
    {
        /* Type filter keeps world-replay forms only — UI model previews come through inUI(),
         * model blocks and items through other types. The picking pass is skipped so editor
         * clicks still select distant actors. A zero camera position means no camera context
         * this frame, so fail open rather than cull by distance from the origin. */
        boolean active = LodSettings.enabled.get()
            && !context.ui
            && !context.isPicking()
            && context.camera.position.lengthSquared() != 0;
        Camera lodCamera = context.camera;
        Camera filmCamera = null;

        if (LodSettings.filmCameraOnly.get())
        {
            filmCamera = getBBSFilmCamera();

            if (filmCamera != null)
            {
                lodCamera = filmCamera;
            }
        }

        if (active)
        {
            /* The frustum the render actually uses this frame: while a BBS editor is open the
             * world keeps Minecraft's own projection (BBS only replaces the framebuffer once no
             * editor is on screen), so a film camera's clip fov and the export's video size are
             * guesses that drift from what the user sees - a 70 clip fov against a 42 option
             * fov culls nothing the frustum should, and vice versa. The live projection is the
             * editor preview and the video export alike, so whatever the render uses, the cull
             * uses too. The film camera still supplies position and rotation through boneToWorld
             * below; only the shape of its cone comes from here. */
            Matrix4f projection = RenderSystem.getProjectionMatrix();
            float halfHeight;
            float aspect;

            if (projection.m22() < 0F)
            {
                halfHeight = 1F / projection.m11();
                aspect = projection.m11() / projection.m00();
            }
            else
            {
                halfHeight = (float) Math.tan(lodCamera.fov / 2F);
                Framebuffer buffer = MinecraftClient.getInstance().getFramebuffer();

                aspect = buffer.textureHeight > 0 ? (float) buffer.textureWidth / buffer.textureHeight : 1F;
            }

            LodState.viewHalfWidth = halfHeight * aspect;
            LodState.viewHalfHeight = halfHeight;
            /* The bone mixin's stack is the render camera's view space. Recast it into the film
             * camera's view space so the size and frustum tests judge bones by the shot's camera
             * even while the world is drawn through the free camera. Both rotations are
             * orthonormal, so the transpose is the inverse; with coincident cameras this is the
             * identity transform and the rule behaves exactly as before. */
            if (filmCamera != null)
            {
                RENDER_VIEW.set(context.camera.view).transpose();
                LodState.boneToWorld.set(filmCamera.updateView()).mul(RENDER_VIEW);

                OFFSET.set(context.camera.position).sub((float) filmCamera.position.x, (float) filmCamera.position.y, (float) filmCamera.position.z);
                filmCamera.updateView().transformPosition(OFFSET);
                LodState.boneToWorld.setTranslation(OFFSET);
                LodState.filmCameraDriven = true;
            }
            else
            {
                LodState.boneToWorld.identity();
                LodState.filmCameraDriven = false;
            }
        }
        else
        {
            LodState.viewHalfWidth = 0F;
            LodState.viewHalfHeight = 0F;
            LodState.boneToWorld.identity();
            LodState.filmCameraDriven = false;
        }

        int tier = active ? computeTier(form, context, lodCamera) : 0;

        applyVisibility(form, tier);

        if (LodSettings.debug.get())
        {
            frames++;

            if (tier == 1) tier1++;
            else if (tier == 2) tier2++;

            /* The BBS camera's frustum, once per frame, through this form's stack. */
            if (active && LodDebug.cameraPending())
            {
                LodDebug.drawCamera(context.stack, lodCamera);
            }

            if (frames % 200 == 0)
            {
                LOGGER.info("bbslod: frames={} tier1={} tier2={} mixinHits={} occHits={}", frames, tier1, tier2, LodState.mixinHits, LodState.occlusionHits);

                /* mixinHits staying at 0 while tier1 > 0 means the Phase 4 mixin is not
                 * applying — the BBS render target moved and bone culling is silently off. */
                if (!warned && tier1 > 0 && LodState.mixinHits == 0)
                {
                    warned = true;
                    LOGGER.warn("bbslod: tier 1 is being computed but the CubicVAORenderer mixin has never hit — the BBS render target moved, so bone culling is inactive (distance culling still works)");
                }
            }
        }

        LodState.push(tier, active ? form : null);
    }

    private static void after(Form form, FormRenderingContext context)
    {
        LodState.pop();
    }

    /**
     * Hide or restore the form without clobbering the timeline.
     *
     * <p>{@code form.visible.get()} is whatever the track wrote this frame; if the track already
     * hid the form, LOD leaves it alone. Restoring happens here for a body part whose tier dropped
     * back below 1, and in bulk at {@link #clearOverrides(BaseFilmController)} — not in
     * {@code AFTER}, which fires before this very form's shadow and name tag are drawn.</p>
     */
    private static void applyVisibility(Form form, int tier)
    {
        if (!form.visible.get())
        {
            return;
        }

        if (tier == 2)
        {
            form.visible.setRuntimeValue(Boolean.FALSE);
            touched.add(form);
        }
        else if (touched.remove(form))
        {
            form.visible.setRuntimeValue(null);
        }
    }

    /**
     * DH-style distance shells with a zoom bias: a narrow fov makes the effective distance
     * <em>smaller</em>, so a telephoto shot keeps detail at the same world distance.
     */
    private static int computeTier(Form form, FormRenderingContext context, Camera camera)
    {
        /* Camera-locked forms never distance-cull — BBS's own isCulled bypasses them too. */
        if (form.anchor.get().hasTarget())
        {
            return 0;
        }

        IEntity entity = context.entity;

        double distance = camera.getRelative(entity.getX(), entity.getY(), entity.getZ()).length();

        if (LodSettings.fovBias.get())
        {
            /* Clamped on purpose. Below the reference fov (a telephoto shot) the factor shrinks
             * the effective distance, so detail survives further out — that is the point of the
             * bias. Above it (a wide-angle shot) an unclamped factor would double the effective
             * distance and cull actors that are plainly visible at the edges of the frame, so it
             * is held at 1: a wide shot never culls earlier than the raw distance. */
            float factor = (float) (Math.tan(camera.fov / 2F) / Math.tan(REFERENCE_FOV / 2F));

            distance *= Math.min(1F, factor);
        }

        float cullDistance = perForm(form, BBSLod.CULL, LodSettings.cullDistance.get());
        float simplifyDistance = perForm(form, BBSLod.SIMPLIFY, LodSettings.simplifyDistance.get());

        if (cullDistance > 0F && distance >= cullDistance)
        {
            return 2;
        }
        else if (simplifyDistance > 0F && distance >= simplifyDistance)
        {
            return 1;
        }

        return 0;
    }

    /** A per-form override of {@code >= 0} wins; anything else inherits the global setting. */
    private static float perForm(Form form, String id, float global)
    {
        BaseValue value = form.get(id);

        if (value instanceof ValueFloat override)
        {
            float distance = override.get();

            if (distance >= 0F)
            {
                return distance;
            }
        }

        return global;
    }

    private static void onRenderAfter(BaseFilmController controller, WorldRenderContext context)
    {
        clearOverrides(controller);

        /* The depth buffer here holds every opaque block and every form drawn this frame, which
         * is exactly the set of things that can hide a bone; next frame's bone tests read it. */
        LodOcclusion.capture();
        LodDebug.endFrame();
    }

    private static Camera getBBSFilmCamera()
    {
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (!(menu instanceof UIDashboard dashboard))
        {
            return null;
        }

        UIFilmPanel panel = dashboard.getPanels().getPanel(UIFilmPanel.class);

        if (panel == null)
        {
            return null;
        }

        RunnerCameraController runner = panel.getRunner();

        if (runner == null || runner.getContext().clips == null)
        {
            return null;
        }

        /* The runner's pose is where the film's camera sits at the cursor. apply() fills it even
         * in free mode, where it never reaches the render camera — exactly the case the gate
         * exists to inspect. */
        runner.getPosition().apply(FILM_CAMERA);
        FILM_CAMERA.updateView();

        return FILM_CAMERA;
    }

    private static void onShutdown(BaseFilmController controller)
    {
        clearOverrides(controller);
        LodOcclusion.reset();
    }

    /**
     * Clears every runtime override the controller's root forms carry.
     *
     * <p>The shadow and name tag checks in {@code FilmEntityRenderer} run after
     * {@code FormUtilsClient.render} within the same entity render, so they see {@code visible ==
     * false} for the culled form too; and nothing leaks into the next frame's track application.</p>
     */
    private static void clearOverrides(BaseFilmController controller)
    {
        for (IEntity entity : controller.getEntities().values())
        {
            Form form = entity.getForm();

            if (form != null)
            {
                form.visible.setRuntimeValue(null);
            }
        }

        touched.clear();
    }
}
