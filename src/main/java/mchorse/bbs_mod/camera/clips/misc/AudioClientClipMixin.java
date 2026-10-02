package mchorse.bbs_mod.camera.clips.misc;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.ClipContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AudioClientClip.class, remap = false)
public abstract class AudioClientClipMixin
{
    @Inject(method = "applyClip", at = @At("HEAD"), cancellable = true)
    private void bbslezy$applyClip(ClipContext context, Position position, CallbackInfo ci)
    {
        try
        {
            AudioClip self = (AudioClip) (Object) this;
            boolean reverse = LezyAudioReverse.isEnabled(self);
            boolean keyed = self.envelope != null && self.envelope.keyframes.get();

            if (!reverse && !keyed)
            {
                return;
            }

            float t = context.relativeTick + context.transition;
            float gain = self.volume.get() * (keyed ? self.envelope.factorEnabled(self.duration.get(), t) : 1F);

            if (!reverse)
            {
                AudioClientClip.scheduleAudio(context, self, gain, 0F);
            }
            else
            {
                Link link = self.audio.get();

                if (link != null)
                {
                    SoundBuffer rev = LezyAudioReverse.getReversed(link);
                    float tickTime = t / 20F;

                    if (rev == null || context.relativeTick >= self.duration.get() || tickTime < 0F)
                    {
                        AudioClientClip.getPlayback(context).put(self, new AudioClientClip.Playback(link, -1F, gain));
                    }
                    else
                    {
                        float q = rev.getDuration()
                            - TimeUtils.toSeconds(self.offset.get())
                            - TimeUtils.toSeconds(self.duration.get())
                            + tickTime;

                        AudioClientClip.getPlayback(context).put(self, new AudioClientClip.Playback(link, Math.max(0F, q), gain));
                    }
                }
            }

            ci.cancel();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
