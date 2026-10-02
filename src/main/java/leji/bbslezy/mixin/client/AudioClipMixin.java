package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AudioClip.class, remap = false)
public abstract class AudioClipMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$init(CallbackInfo ci)
    {
        try
        {
            ValueBoolean reverse = new ValueBoolean(LezyAudioReverse.KEY_REVERSE, false);
            ((ValueGroup) (Object) this).add(reverse);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
