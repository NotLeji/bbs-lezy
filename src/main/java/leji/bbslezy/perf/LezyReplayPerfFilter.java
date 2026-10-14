package leji.bbslezy.perf;

import java.util.Collections;
import java.util.List;

import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;

/**
 * Hot-path adapter called from the mixin layer. All of this runs on a caught
 * path — a throw at any point means the caller must simply see "run all replays".
 */
public final class LezyReplayPerfFilter
{
    public static List<Replay> getEffectiveFrameReplays(BaseFilmController controller)
    {
        List<Replay> replays = controller.film == null ? Collections.emptyList() : controller.film.replays.getList();
        List<Replay> filtered = filterFrame(controller, controller.getTick(), replays);

        return filtered != null ? filtered : replays;
    }

    public static List<Replay> getEffectiveTickReplays(BaseFilmController controller)
    {
        List<Replay> replays = controller.film == null ? Collections.emptyList() : controller.film.replays.getList();
        List<Replay> filtered = filterTick(controller, controller.getTick(), replays);

        return filtered != null ? filtered : replays;
    }

    public static List<Replay> filterFrame(BaseFilmController controller, int tick, List<Replay> original)
    {
        if (!enabled() || controller.exception >= 0)
        {
            /* exception >= 0 = recorder active; exception is an index into the full list,
               and filtering shifts loop indices — fail-open completely during recording. */
            return null;
        }

        try
        {
            return LezyPerfPolicy.filterFrame(controller, tick);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    public static List<Replay> filterTick(BaseFilmController controller, int tick, List<Replay> original)
    {
        if (!enabled() || controller.exception >= 0)
        {
            return null;
        }

        try
        {
            return LezyPerfPolicy.filterTick(controller, tick);
        }
        catch (Throwable t)
        {
            t.printStackTrace();

            return null;
        }
    }

    private static boolean enabled()
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

    private LezyReplayPerfFilter()
    {}
}
