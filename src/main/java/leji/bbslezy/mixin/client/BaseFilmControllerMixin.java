package leji.bbslezy.mixin.client;

import java.util.Collections;
import java.util.List;

import leji.bbslezy.perf.LezyReplayPerfFilter;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BaseFilmController.class, remap = false)
public abstract class BaseFilmControllerMixin
{
    @Redirect(method = "startRenderFrame(F)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/film/BaseFilmController;replays()Ljava/util/List;", remap = false))
    private List<Replay> bbslezy$perfFrameFilter(BaseFilmController controller)
    {
        List<Replay> replays = controller.film == null ? Collections.emptyList() : controller.film.replays.getList();
        List<Replay> filtered = LezyReplayPerfFilter.filterFrame(controller, controller.getTick(), replays);

        return filtered != null ? filtered : replays;
    }

    @Redirect(method = "updateEntities(I)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/film/BaseFilmController;replays()Ljava/util/List;", remap = false))
    private List<Replay> bbslezy$perfTickFilter(BaseFilmController controller)
    {
        List<Replay> replays = controller.film == null ? Collections.emptyList() : controller.film.replays.getList();
        List<Replay> filtered = LezyReplayPerfFilter.filterTick(controller, controller.getTick(), replays);

        return filtered != null ? filtered : replays;
    }
}
