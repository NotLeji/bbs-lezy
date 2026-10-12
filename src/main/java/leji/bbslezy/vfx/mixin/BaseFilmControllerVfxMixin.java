package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import leji.bbslezy.vfx.forms.LezyWindForm;

/**
 * Prevents frustum culling on zonal VFX forms (such as LezyWindForm) during film replay rendering.
 * Zonal wind spans a wide area around the actor; when the actor's position leaves the camera frustum,
 * the volume and leaves should continue rendering over the zone.
 */
@Mixin(value = BaseFilmController.class, remap = false)
public abstract class BaseFilmControllerVfxMixin
{
    @Inject(method = "isCulled", at = @At("HEAD"), cancellable = true)
    private void bbslezy$exemptWindFromCulling(Frustum frustum, Replay replay, IEntity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if (entity != null)
        {
            Form form = entity.getForm();

            if (form instanceof LezyWindForm)
            {
                cir.setReturnValue(false);
            }
        }
    }
}
