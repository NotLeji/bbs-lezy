package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.resources.Link;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SoundManager.class, remap = false)
public abstract class SoundManagerMixin
{
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void bbslezy$get(Link link, boolean includeWaveform, CallbackInfoReturnable<SoundBuffer> cir)
    {
        if (link != null && LezyAudioReverse.REVERSED_SOURCE.equals(link.source))
        {
            SoundBuffer buffer = LezyAudioReverse.getReversed(link);

            if (buffer != null)
            {
                cir.setReturnValue(buffer);
            }
        }
    }
}
