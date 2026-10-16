package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.forms.FormUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Files the {@code follow_offset}, {@code smear_frames}, and {@code motion_lines} form properties under the POSE category.
 */
@Mixin(value = FormUtils.class, remap = false)
public abstract class FormUtilsPoseMixin
{
    @Inject(method = "isPoseProperty", at = @At("HEAD"), cancellable = true)
    private static void bbslezy$smearIsPose(String name, CallbackInfoReturnable<Boolean> cir)
    {
        if (name != null && (name.startsWith("follow_offset") || name.equals("smear_frames") || name.equals("motion_lines")))
        {
            cir.setReturnValue(true);
        }
    }
}
