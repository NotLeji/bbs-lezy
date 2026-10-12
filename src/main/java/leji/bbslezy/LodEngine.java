package leji.bbslezy;

import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    /** Union of every live controller's culled replay keys (`filmId + replayId`), for real ActorEntity bodies. */
    private static final Set<String> culledReplays = new HashSet<>();

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

            return;
        }

        /* Real ActorEntity bodies (world playback via Right Ctrl) carry a different Form instance
         * than the controller's copy, so identity matching above never fires for them. Match by replay key. */
        if (context.entity instanceof MCEntity mc && mc.getMcEntity() instanceof ActorEntity actor && !actor.getReplayId().isEmpty())
        {
            if (culledReplays.contains(actor.getFilmId() + "\0" + actor.getReplayId()))
            {
                form.visible.setRuntimeValue(Boolean.FALSE);
                touched.add(form);
            }
        }
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

        boolean enabled = LodSettings.enabled.get();
        int limit = LodSettings.renderLimit.get();

        budgetValid = enabled && limit > 0;

        if (!budgetValid)
        {
            if (!states.isEmpty())
            {
                states.clear();
                rebuildUnions();
            }

            return;
        }

        ControllerState state = states.get(controller);

        if (state == null)
        {
            state = new ControllerState();
            states.put(controller, state);
        }

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double focus = LodSettings.focusDistance.get();
        int entityCount = controller.getEntities().size();
        int tick = controller.getTick();

        double dx = camPos.x - state.lastCamX;
        double dy = camPos.y - state.lastCamY;
        double dz = camPos.z - state.lastCamZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        float dYaw = Math.abs(yaw - state.lastYaw);
        float dPitch = Math.abs(pitch - state.lastPitch);

        boolean tickChanged = tick != state.lastTick;
        boolean settingsChanged = enabled != state.lastEnabled || limit != state.lastLimit || focus != state.lastFocus || entityCount != state.lastEntityCount || tickChanged;
        boolean camMoved = distSq >= MOVE_THRESHOLD_SQ;
        boolean camRotated = dYaw >= ROTATION_THRESHOLD || dPitch >= ROTATION_THRESHOLD;

        /* Skip re-ranking only when camera is completely stationary and settings are unchanged */
        if (!settingsChanged && !camMoved && !camRotated && !state.candidates.isEmpty())
        {
            return;
        }

        state.lastCamX = camPos.x;
        state.lastCamY = camPos.y;
        state.lastCamZ = camPos.z;
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
        for (Map.Entry<String, IEntity> entry : controller.getEntities().entrySet())
        {
            IEntity entity = entry.getValue();
            Form form = entity.getForm();

            if (form == null || form.anchor.get().hasTarget() || !form.visible.get())
            {
                continue;
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

            double fx = ex - camPos.x;
            double fy = ey - camPos.y;
            double fz = ez - camPos.z;
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
                state.candidates.get(count).set(form, replayKey(controller, entry.getKey()), score);
            }
            else
            {
                state.candidates.add(new Candidate(form, replayKey(controller, entry.getKey()), score));
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
        state.culledKeys.clear();
        int effectiveLimit = Math.min(limit, count);
        for (int i = effectiveLimit; i < count; i++)
        {
            Candidate candidate = state.candidates.get(i);
            state.culledForms.add(candidate.form);
            if (candidate.replayKey != null)
            {
                state.culledKeys.add(candidate.replayKey);
            }
        }

        rebuildUnions();
    }

    private static void onShutdown(BaseFilmController controller)
    {
        clearOverrides();
        states.remove(controller);
        rebuildUnions();
    }

    private static String replayKey(BaseFilmController controller, String replayId)
    {
        if (controller == null || controller.film == null || replayId == null)
        {
            return null;
        }

        return controller.film.getId() + "\0" + replayId;
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

    private static void rebuildUnions()
    {
        culled.clear();
        culledReplays.clear();

        for (ControllerState state : states.values())
        {
            culled.addAll(state.culledForms);
            culledReplays.addAll(state.culledKeys);
        }
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

    private static class ControllerState
    {
        public final List<Candidate> candidates = new ArrayList<>();
        public final Set<Form> culledForms = Collections.newSetFromMap(new IdentityHashMap<>());
        public final Set<String> culledKeys = new HashSet<>();
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
        public String replayKey;
        public double score;

        public Candidate(Form form, String replayKey, double score)
        {
            this.form = form;
            this.replayKey = replayKey;
            this.score = score;
        }

        public void set(Form form, String replayKey, double score)
        {
            this.form = form;
            this.replayKey = replayKey;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate other)
        {
            return Double.compare(this.score, other.score);
        }
    }
}
