package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.Map;

@Mixin(value = AudioClientClip.class, remap = false)
public abstract class AudioClientClipMixin
{
    private static Constructor<?> bbslezy$playbackConstructor;

    @Inject(method = "applyClip", at = @At("HEAD"), cancellable = true)
    private void bbslezy$applyClip(ClipContext context, Position position, CallbackInfo ci)
    {
        try
        {
            AudioClip self = (AudioClip) (Object) this;
            boolean reverse = LezyAudioReverse.isEnabled(self);
            KeyframeChannel<Double> volumeChannel = LezyAudioReverse.getVolumeChannel(self);
            boolean hasVolumeKeyframes = volumeChannel != null && !volumeChannel.isEmpty();
            boolean keyed = self.envelope != null && self.envelope.keyframes.get();

            if (!reverse && !hasVolumeKeyframes && !keyed)
            {
                return;
            }

            float t = context.relativeTick + context.transition;
            float gain = LezyAudioReverse.computeGain(self, t);

            if (!reverse)
            {
                AudioClientClip.scheduleAudio(context, self, gain, 0F);
            }
            else
            {
                Link link = self.audio.get();

                if (link != null)
                {
                    Link revLink = LezyAudioReverse.toReversedLink(link);
                    SoundBuffer rev = LezyAudioReverse.getReversed(revLink);
                    float tickTime = t / 20F;
                    Map<Object, Object> playback = (Map) AudioClientClip.getPlayback(context);

                    if (bbslezy$playbackConstructor == null)
                    {
                        for (Class<?> clazz : AudioClientClip.class.getDeclaredClasses())
                        {
                            if (clazz.getSimpleName().equals("Playback"))
                            {
                                bbslezy$playbackConstructor = clazz.getDeclaredConstructor(Link.class, float.class, float.class);
                                bbslezy$playbackConstructor.setAccessible(true);
                                break;
                            }
                        }
                    }

                    if (bbslezy$playbackConstructor != null)
                    {
                        if (rev == null || context.relativeTick >= self.duration.get() || tickTime < 0F)
                        {
                            Object pb = bbslezy$playbackConstructor.newInstance(revLink, -1F, gain);
                            playback.put(self, pb);
                        }
                        else
                        {
                            float q = rev.getDuration()
                                - TimeUtils.toSeconds(self.offset.get())
                                - TimeUtils.toSeconds(self.duration.get())
                                + tickTime;

                            Object pb = bbslezy$playbackConstructor.newInstance(revLink, Math.max(0F, q), gain);
                            playback.put(self, pb);
                        }
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
