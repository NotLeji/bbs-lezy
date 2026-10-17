package leji.bbslezy.mixin.client;

import leji.bbslezy.LodSettings;
import leji.bbslezy.perf.LezyPerfPolicy;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.FilmEntityRenderer;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FilmEntityRenderer.class, remap = false)
public abstract class FilmEntityRendererMixin
{
    @Redirect(method = "renderEntity(Lmchorse/bbs_mod/film/FilmControllerContext;)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/settings/values/numeric/ValueBoolean;get()Ljava/lang/Object;", ordinal = 0, remap = false))
    private static Object bbslezy$suppressFarShadow(ValueBoolean prop, FilmControllerContext context)
    {
        return bbslezyVisibleHolds(context, prop);
    }

    @Redirect(method = "renderEntity(Lmchorse/bbs_mod/film/FilmControllerContext;)V", at = @At(value = "INVOKE",
        target = "Lmchorse/bbs_mod/settings/values/numeric/ValueBoolean;get()Ljava/lang/Object;", ordinal = 2, remap = false))
    private static Object bbslezy$suppressFarNameTag(ValueBoolean prop, FilmControllerContext context)
    {
        return bbslezyVisibleHolds(context, prop);
    }

    private static Boolean bbslezyVisibleHolds(FilmControllerContext context, ValueBoolean prop)
    {
        boolean visible = prop != null && Boolean.TRUE.equals(prop.get());

        if (!visible)
        {
            return Boolean.FALSE;
        }

        try
        {
            if (LodSettings.perf_enabled != null
                && LodSettings.perf_enabled.get()
                && !BBSRendering.isIrisShadowPass()
                && LezyPerfPolicy.isBadgeSuppressed(context.replay, context.entity))
            {
                return Boolean.FALSE;
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }

        return Boolean.TRUE;
    }
}
