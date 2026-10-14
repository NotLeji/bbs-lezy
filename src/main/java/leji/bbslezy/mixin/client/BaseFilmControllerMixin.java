package leji.bbslezy.mixin.client;

import java.util.Collections;
import java.util.List;

import leji.bbslezy.perf.LezyReplayPerfFilter;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.client.render.Frustum;
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

    @Redirect(method = "render(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/film/BaseFilmController;replays()Ljava/util/List;", remap = false))
    private List<Replay> bbslezy$perfRenderFilter(BaseFilmController controller)
    {
        List<Replay> replays = controller.film == null ? Collections.emptyList() : controller.film.replays.getList();
        List<Replay> filtered = LezyReplayPerfFilter.filterFrame(controller, controller.getTick(), replays);

        return filtered != null ? filtered : replays;
    }

    @Redirect(method = "render(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/film/BaseFilmController;isCulled(Lnet/minecraft/class_4604;Lmchorse/bbs_mod/film/replays/Replay;Lmchorse/bbs_mod/forms/entities/IEntity;)Z", remap = false))
    private boolean bbslezy$optimizedIsCulled(BaseFilmController controller, Frustum frustum, Replay replay, IEntity entity)
    {
        Form form = entity.getForm();

        if (form == null || form.anchor.get().hasTarget())
        {
            return false;
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        try
        {
            return !((FrustumInvoker) frustum).bbslezy$isVisible(x - 32.0, y - 32.0, z - 32.0, x + 32.0, y + 32.0, z + 32.0);
        }
        catch (Throwable t)
        {
            return false;
        }
    }
}
