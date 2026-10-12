package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.data.migration.FormStableIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = FormStableIds.class, remap = false)
public abstract class FormStableIdsMixin
{
    @Inject(method = "rewriteTrackKey", at = @At("HEAD"), cancellable = true)
    private static void bbslezy$guardEmptyKey(String key, Map<String, String> mapping, CallbackInfoReturnable<String> cir)
    {
        if (key == null || key.isEmpty())
        {
            cir.setReturnValue(key);
        }
    }
}
