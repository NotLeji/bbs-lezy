package bbslezy.mixin.client;

import bbslezy.video.LezyVideoSettingsHelper;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.VideoRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VideoRecorder.class, remap = false)
public abstract class VideoRecorderMixin
{
    @Redirect(
        method = "startRecording",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/settings/values/core/ValueString;get()Ljava/lang/String;"
        )
    )
    private String bbslezy$applyCustomCodecAndCqp(ValueString valueString)
    {
        String original = valueString.get();

        return LezyVideoSettingsHelper.apply(original);
    }
}
