package leji.bbslezy.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import leji.bbslezy.LodEngine;
import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

/**
 * State bridge connecting BBS film controllers and entities to the pure
 * {@link PerfDecision} evaluation budget.
 */
public final class LezyPerfPolicy
{
    private static final Map<BaseFilmController, ControllerState> states = new IdentityHashMap<>();

    private static final class ControllerState
    {
        final List<Replay> filteredTick = new ArrayList<>();
        final List<Replay> filteredFrame = new ArrayList<>();
        final Set<String> anchoredTargetIds = new HashSet<>();

        int lastReplayCount = -1;
        Film lastFilm;
    }

    private record Thresholds(int throttleDist, int skipDist, double throttleDistSq, double skipDistSq)
    {}

    /**
     * Filters replays for the tick evaluation path (updateEntities).
     * Returns null if policy is disabled or fail-open (caller uses original list).
     */
    public static List<Replay> filterTick(BaseFilmController controller, int tick)
    {
        try
        {
            if (!isEnabled() || controller == null || controller.film == null || controller.exception >= 0)
            {
                return null;
            }

            return filterInternal(controller, tick, false);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    /**
     * Filters replays for the frame render preparation path (startRenderFrame).
     * Returns null if policy is disabled or fail-open (caller uses original list).
     */
    public static List<Replay> filterFrame(BaseFilmController controller, int tick)
    {
        try
        {
            if (!isEnabled() || controller == null || controller.film == null || controller.exception >= 0)
            {
                return null;
            }

            return filterInternal(controller, tick, true);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    /**
     * Checks whether shadow and nametag badges should be suppressed for a distant replay.
     */
    public static boolean isBadgeSuppressed(Replay replay, IEntity entity)
    {
        try
        {
            if (!isEnabled())
            {
                return false;
            }

            if (replay == null && entity == null)
            {
                return false;
            }

            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null)
            {
                return false;
            }

            Camera camera = mc.gameRenderer.getCamera();
            Vec3d camPos = camera.getPos();

            double[] pos = new double[] {0, 0, 0};
            resolveEntityPosition(replay, entity, pos, 0, 0, 0);

            double dx = pos[0] - camPos.x;
            double dy = pos[1] - camPos.y;
            double dz = pos[2] - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            Thresholds th = getThresholds();
            Form form = resolveForm(replay, entity);
            boolean inFrustum = form == null || !LodEngine.getCulledForms().contains(form);
            boolean cameraLocked = form != null && form.anchor.get().hasTarget();
            boolean shellActor = replay != null && replay.actor.get();

            PerfDecision.Inputs inputs = new PerfDecision.Inputs(
                distSq,
                inFrustum,
                shellActor,
                false,
                cameraLocked,
                false,
                0L,
                4,
                th.throttleDistSq,
                th.skipDistSq
            );

            return PerfDecision.isBadge(inputs);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return false;
        }
    }

    /**
     * Clears cached state for the given controller.
     */
    public static void clear(BaseFilmController controller)
    {
        try
        {
            if (controller != null)
            {
                states.remove(controller);
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    /**
     * Hook called on film shutdown lifecycle event.
     */
    public static void onFilmShutdown(BaseFilmController controller)
    {
        clear(controller);
    }

    private static boolean isEnabled()
    {
        try
        {
            return LodSettings.perf_enabled != null
                && LodSettings.perf_enabled.get()
                && !BBSRendering.isIrisShadowPass();
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private static List<Replay> filterInternal(BaseFilmController controller, int tick, boolean frameMode)
    {
        List<Replay> replays = controller.film.replays.getList();

        if (replays == null || replays.isEmpty())
        {
            return replays;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null)
        {
            return null;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        ControllerState state = states.computeIfAbsent(controller, (k) -> new ControllerState());
        updateAnchorCache(state, controller, replays);

        Thresholds th = getThresholds();
        int throttleTicks = LodSettings.perf_throttle_ticks != null ? LodSettings.perf_throttle_ticks.get() : 4;
        int throttleEvery = Math.max(0, throttleTicks);

        Set<Form> culledForms = LodEngine.getCulledForms();
        Map<String, Integer> actors = controller.getActors();
        Map<String, IEntity> entities = controller.getEntities();

        List<Replay> resultList = frameMode ? state.filteredFrame : state.filteredTick;
        resultList.clear();

        double[] pos = new double[3];

        for (int i = 0; i < replays.size(); i++)
        {
            Replay replay = replays.get(i);
            IEntity entity = entities != null ? entities.get(replay.getId()) : null;

            resolveEntityPosition(replay, entity, pos, camPos.x, camPos.y, camPos.z);

            double dx = pos[0] - camPos.x;
            double dy = pos[1] - camPos.y;
            double dz = pos[2] - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            Form form = replay.form.get();
            boolean inFrustum = form == null || !culledForms.contains(form);
            boolean shellActor = replay.actor.get() && actors != null && actors.containsKey(replay.getId());
            boolean anchoredByOther = state.anchoredTargetIds.contains(replay.getId());
            boolean cameraLocked = form != null && form.anchor.get().hasTarget();
            boolean exception = controller.exception == i;
            long replayTicks = replay.getTick(tick);

            PerfDecision.Inputs inputs = new PerfDecision.Inputs(
                distSq,
                inFrustum,
                shellActor,
                anchoredByOther,
                cameraLocked,
                exception,
                replayTicks,
                throttleEvery,
                th.throttleDistSq,
                th.skipDistSq
            );

            PerfDecision.Verdict verdict = frameMode
                ? PerfDecision.evaluateFrame(inputs)
                : PerfDecision.evaluate(inputs);

            if (verdict != PerfDecision.Verdict.SKIP)
            {
                resultList.add(replay);
            }
        }

        return resultList;
    }

    private static Thresholds getThresholds()
    {
        int throttleDist = LodSettings.perf_throttle_distance != null ? LodSettings.perf_throttle_distance.get() : 32;
        int skipDist = LodSettings.perf_skip_distance != null ? LodSettings.perf_skip_distance.get() : 96;

        if (skipDist < throttleDist)
        {
            int temp = skipDist;
            skipDist = throttleDist;
            throttleDist = temp;
        }

        return new Thresholds(
            throttleDist,
            skipDist,
            (double) throttleDist * throttleDist,
            (double) skipDist * skipDist
        );
    }

    private static void resolveEntityPosition(Replay replay, IEntity entity, double[] out, double defX, double defY, double defZ)
    {
        double ex = defX;
        double ey = defY;
        double ez = defZ;

        if (entity != null)
        {
            ex = entity.getX();
            ey = entity.getY();
            ez = entity.getZ();
        }

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

        out[0] = ex;
        out[1] = ey;
        out[2] = ez;
    }

    private static Form resolveForm(Replay replay, IEntity entity)
    {
        if (replay != null)
        {
            return replay.form.get();
        }
        else if (entity != null)
        {
            return entity.getForm();
        }

        return null;
    }

    private static void updateAnchorCache(ControllerState state, BaseFilmController controller, List<Replay> replays)
    {
        if (state.lastFilm == controller.film && state.lastReplayCount == replays.size())
        {
            return;
        }

        state.anchoredTargetIds.clear();

        for (Replay replay : replays)
        {
            Form form = replay.form.get();

            if (form != null)
            {
                Anchor anchor = form.anchor.get();

                if (anchor != null && anchor.hasTarget() && anchor.replay != null && !anchor.replay.isEmpty())
                {
                    state.anchoredTargetIds.add(anchor.replay);
                }
            }
        }

        state.lastFilm = controller.film;
        state.lastReplayCount = replays.size();
    }

    private LezyPerfPolicy()
    {}
}
