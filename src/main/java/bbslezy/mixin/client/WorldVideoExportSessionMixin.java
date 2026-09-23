package bbslezy.mixin.client;

import mchorse.bbs_mod.film.WorldVideoExportSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorldVideoExportSession.class, remap = false)
public abstract class WorldVideoExportSessionMixin
{
    /**
     * Disable shouldAbortWarmup check so asynchronous film loading does not cancel F6 export.
     */
    @Inject(method = "shouldAbortWarmup", at = @At("HEAD"), cancellable = true)
    private void bbslezy$disableAbortWarmup(CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }
}
