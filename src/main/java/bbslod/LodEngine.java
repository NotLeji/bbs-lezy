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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            && context.type == FormRenderType.ENTITY
            && !context.ui
            && !context.isPicking()
            && context.camera.position.lengthSquared() != 0;

        int tier = active ? computeTier(form, context) : 0;

        applyVisibility(form, tier);

        if (LodSettings.debug.get())
        {
            frames++;

            if (tier == 1) tier1++;
            else if (tier == 2) tier2++;

            /* The BBS camera's frustum, once per frame, through this form's stack. */
            if (active && LodDebug.cameraPending())
            {
                LodDebug.drawCamera(context.stack, context.camera);
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
    private static int computeTier(Form form, FormRenderingContext context)
    {
        /* Camera-locked forms never distance-cull — BBS's own isCulled bypasses them too. */
        if (form.anchor.get().hasTarget())
        {
            return 0;
        }

        Camera camera = context.camera;
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
