package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
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
            ValueGroup group = (ValueGroup) (Object) this;
            ValueBoolean reverse = new ValueBoolean(LezyAudioReverse.KEY_REVERSE, false);
            KeyframeChannel<Double> volumeChannel = new KeyframeChannel<>(LezyAudioReverse.KEY_VOLUME_CHANNEL, KeyframeFactories.DOUBLE);

            group.add(reverse);
            group.add(volumeChannel);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
