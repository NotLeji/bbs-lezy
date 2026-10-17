package leji.bbslezy.mixin.client;

import leji.bbslezy.perf.ui.LezyDataKeysCache;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.manager.BaseManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BaseManager.class, remap = false)
public class BaseManagerMutationsMixin
{
    @Inject(method = "save(Ljava/lang/String;Lmchorse/bbs_mod/data/types/MapType;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateSave(String id, MapType data, CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((BaseManager<?>) (Object) this);
    }
}
