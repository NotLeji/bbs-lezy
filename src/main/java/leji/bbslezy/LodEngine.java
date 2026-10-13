package leji.bbslezy;

import leji.bbslezy.ui.LezyEntityCulling;
import leji.bbslezy.ui.LezyPov;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * Caps how many of a scene's forms get drawn per frame with focus-distance support.
 *
 * <p>Uses camera look-direction (frustum weighting) so that actors in the camera's field of view
 * are prioritized over actors behind the camera, eliminating ghost renders and missing replays.
 * Tracks both position and rotation with a sensitive deadband for smooth, responsive movement.
 * The model of a selected replay obeys LOD and focus culling rules, while its transform gizmo
 * remains visible in the editor.</p>
 */
public class LodEngine
{
    /** Forms whose visible runtimeValue was set to false this frame, restored on RENDER_AFTER. */
    private static final Set<Form> touched = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Union of every live controller's culled forms. `before()` has no controller, so it reads this. */
    private static final Set<Form> culled = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Per-controller ranking state. A single shared state thrashes when two controllers render one frame. */
    private static final Map<BaseFilmController, ControllerState> states = new IdentityHashMap<>();

    private static boolean budgetValid = false;

    /** Movement deadband: ~0.1 blocks. */
    private static final double MOVE_THRESHOLD_SQ = 0.01D;

    /** Rotation deadband: 0.5 degrees. */
    private static final float ROTATION_THRESHOLD = 0.5F;

    public static void register()
    {
        FormRenderEvents.BEFORE.register(LodEngine::before);
        FilmEvents.CREATED.register(LodEngine::onCreated);
        FilmEvents.TICK_AFTER.register(LodEngine::onTickAfter);
        FilmEvents.RENDER_AFTER.register(LodEngine::onRenderAfter);
        FilmEvents.SHUTDOWN.register(LodEngine::onShutdown);
    }

    /**
     * Intercepts form rendering: if this form was ranked as culled for the current frame,
     * set its visible runtime value to false so its model, shadow and nametag are skipped.
     * The editor gizmo does not check form.visible, so the transform gizmo remains visible
     * on selected replays even when the model is culled.
     */
    private static void before(Form form, FormRenderingContext context)
    {
        if (context.ui || !budgetValid || !LodSettings.enabled.get() || !form.visible.get() || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        if (culled.contains(form))
        {
            form.visible.setRuntimeValue(Boolean.FALSE);
            touched.add(form);
        }
    }

    /**
     * Seeds the budget the moment a film's scene is built, before the first tick and the
     * first frame — otherwise world playback draws everything once before ranking exists.
     */
    private static void onCreated(BaseFilmController controller)
    {
        rankFromGameCamera(controller);
    }

    /**
     * Re-ranks every tick from the live game camera, so playback that moves actors re-culls
     * before the next frame instead of one frame late.
     */
    private static void onTickAfter(BaseFilmController controller, int ticks)
    {
        rankFromGameCamera(controller);
    }

    /**
     * Ranks this controller's forms by distance/focus to the camera with view-direction weighting.
     */
    private static void onRenderAfter(BaseFilmController controller, WorldRenderContext context)
    {
        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        clearOverrides();

        if (!refreshBudget())
        {
            return;
        }

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        rank(controller, camPos.x, camPos.y, camPos.z, camera.getYaw(), camera.getPitch(), false);
    }

    private static void rankFromGameCamera(BaseFilmController controller)
    {
        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null)
        {
            return;
        }

        if (!refreshBudget())
        {
            return;
        }

        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        /* Tick/created handlers run outside the frame: overrides from the last frame are stale,
         * and the RENDER_AFTER clear hasn't run yet for this one. Force a fresh ranking. */
        rank(controller, camPos.x, camPos.y, camPos.z, camera.getYaw(), camera.getPitch(), true);
    }

    private static boolean refreshBudget()
    {
        boolean enabled = LodSettings.enabled.get();
        int limit = LodSettings.renderLimit.get();

        budgetValid = enabled && limit > 0;

        if (!budgetValid && !states.isEmpty())
        {
            states.clear();
            rebuildUnions();
        }

        return budgetValid;
    }
    private static void rank(BaseFilmController controller, double camX, double camY, double camZ, float yaw, float pitch, boolean force)
    {
        boolean enabled = LodSettings.enabled.get();
        int limit = LodSettings.renderLimit.get();
        ControllerState state = states.get(controller);

        if (state == null)
        {
            state = new ControllerState();
            states.put(controller, state);
        }

        double focus = LodSettings.focusDistance.get();
        int entityCount = controller.getEntities().size();
        int tick = controller.getTick();

        double dx = camX - state.lastCamX;
        double dy = camY - state.lastCamY;
        double dz = camZ - state.lastCamZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        float dYaw = Math.abs(yaw - state.lastYaw);
        float dPitch = Math.abs(pitch - state.lastPitch);

        boolean tickChanged = tick != state.lastTick;
        boolean settingsChanged = enabled != state.lastEnabled || limit != state.lastLimit || focus != state.lastFocus || entityCount != state.lastEntityCount || tickChanged;
        boolean camMoved = distSq >= MOVE_THRESHOLD_SQ;
        boolean camRotated = dYaw >= ROTATION_THRESHOLD || dPitch >= ROTATION_THRESHOLD;

        /* Skip re-ranking only when camera is completely stationary and settings are unchanged */
        if (!force && !settingsChanged && !camMoved && !camRotated && !state.candidates.isEmpty())
        {
            return;
        }

        state.lastCamX = camX;
        state.lastCamY = camY;
        state.lastCamZ = camZ;
        state.lastYaw = yaw;
        state.lastPitch = pitch;
        state.lastFocus = focus;
        state.lastLimit = limit;
        state.lastEnabled = enabled;
        state.lastEntityCount = entityCount;
        state.lastTick = tick;

        /* Compute forward gaze direction from camera yaw and pitch */
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * cosPitch;

        int count = 0;
        Map<String, Replay> replaysById = replaysById(controller);
        Map<String, Integer> actors = controller.getActors();
        for (Map.Entry<String, IEntity> entry : controller.getEntities().entrySet())
        {
            IEntity entity = entry.getValue();
            Form form = entity.getForm();

            if (form == null || form.anchor.get().hasTarget() || !form.visible.get())
            {
                continue;
            }

            /* BBS-POV drives some forms as first-person hands (cancels applyTransforms itself).
             * Never cull those: LOD would fight POV's own visibility handling. */
            if (LezyPov.isSuppressed(form))
            {
                continue;
            }

            /* EntityCulling already skips occluded ActorEntity bodies in world render. An occluded
             * body must not eat LOD quota: skip it here so the budget goes to visible actors. */
            if (actors != null && LezyEntityCulling.isLoaded())
            {
                Integer entityId = actors.get(entry.getKey());

                if (entityId != null && LezyEntityCulling.isOccludedById(entityId))
                {
                    continue;
                }
            }

            Replay replay = replaysById == null ? null : replaysById.get(entry.getKey());
            double ex = entity.getX();
            double ey = entity.getY();
            double ez = entity.getZ();

            if (replay != null && replay.relative.get())
            {
                Vector3d origin = replay.getRelativeOrigin();
                if (origin != null)
                {
                    ex += origin.x;
                    ey += origin.y;
                    ez += origin.z;
                }
            }

            double fx = ex - camX;
            double fy = ey - camY;
            double fz = ez - camZ;
            double dist = Math.sqrt(fx * fx + fy * fy + fz * fz);
            double invDist = dist > 0.0001D ? 1.0D / dist : 0.0D;
            double dot = (fx * lookX + fy * lookY + fz * lookZ) * invDist;
            /* Frustum weighting:
             * dot < 0.1 means the actor is behind or outside the camera's view — heavy penalty so it
             * never steals quota from actors the user is looking at.
             * dot >= 0.1 slightly favors actors toward screen center. */
            double score = score(dist, dot, focus);

            if (count < state.candidates.size())
            {
                state.candidates.get(count).set(form, score);
            }
            else
            {
                state.candidates.add(new Candidate(form, score));
            }
            count++;
        }

        while (state.candidates.size() > count)
        {
            state.candidates.remove(state.candidates.size() - 1);
        }

        state.candidates.sort(null);

        /* Update this controller's culled set for the next frame's BEFORE pass */
        state.culledForms.clear();
        int effectiveLimit = Math.min(limit, count);
        for (int i = effectiveLimit; i < count; i++)
        {
            state.culledForms.add(state.candidates.get(i).form);
        }

        rebuildUnions();
    }

    private static void onShutdown(BaseFilmController controller)
    {
        clearOverrides();
        states.remove(controller);
        rebuildUnions();
    }

    /**
     * ponytail: one HashMap build per re-rank; getById() scans the list so per-entity lookup is O(n²).
     */
    private static Map<String, Replay> replaysById(BaseFilmController controller)
    {
        if (controller == null || controller.film == null)
        {
            return null;
        }

        Map<String, Replay> map = new java.util.HashMap<>();

        for (Replay replay : controller.film.replays.getList())
        {
            map.put(replay.getId(), replay);
        }

        return map;
    }

    static double score(double dist, double dot, double focus)
    {
        double err = focus > 0D ? Math.abs(dist - focus) : dist;

        if (dot < 0.1D)
        {
            return err + 1000.0D * (0.1D - dot);
        }

        return err + (1.0D - dot) * 0.5D;
    }

    /**
     * Clears every runtime override this frame carried.
     */
    private static void clearOverrides()
    {
        for (Form form : touched)
        {
            form.visible.setRuntimeValue(null);
        }

        touched.clear();
    }

    private static void rebuildUnions()
    {
        culled.clear();

        for (ControllerState state : states.values())
        {
            culled.addAll(state.culledForms);
        }
    }

    private static class ControllerState
    {
        public final List<Candidate> candidates = new ArrayList<>();
        public final Set<Form> culledForms = Collections.newSetFromMap(new IdentityHashMap<>());
        public double lastCamX = Double.NaN;
        public double lastCamY = Double.NaN;
        public double lastCamZ = Double.NaN;
        public float lastYaw = Float.NaN;
        public float lastPitch = Float.NaN;
        public double lastFocus = -1D;
        public int lastLimit = -1;
        public boolean lastEnabled = false;
        public int lastEntityCount = -1;
        public int lastTick = -1;
    }

    private static class Candidate implements Comparable<Candidate>
    {
        public Form form;
        public double score;

        public Candidate(Form form, double score)
        {
            this.form = form;
            this.score = score;
        }

        public void set(Form form, double score)
        {
            this.form = form;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate other)
        {
            return Double.compare(this.score, other.score);
        }
    }
}
