package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.audio.AudioRenderer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

@Mixin(value = AudioRenderer.class, remap = false)
public abstract class AudioRendererMixin
{
    @Inject(
        method = "renderAudio",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/audio/Wave;add(Ljava/nio/ByteBuffer;Lmchorse/bbs_mod/audio/Wave;FFFFF)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private static void bbslezy$renderAudioMixClip(
        File file,
        List<AudioClip> clips,
        int sampleRate,
        int totalDuration,
        int from,
        int to,
        CallbackInfoReturnable<Boolean> cir,
        float total,
        Map<AudioClip, Wave> map,
        int byteRate,
        int totalBytes,
        byte[] bytes,
        Wave finalWave,
        ByteBuffer buffer,
        AudioClip clip,
        Wave wave
    )
    {
        try
        {
            boolean keyed = clip.envelope != null && clip.envelope.keyframes.get();
            boolean reverse = LezyAudioReverse.isEnabled(clip);

            if (!keyed && !reverse)
            {
                return;
            }

            LezyAudioReverse.mixClip(
                finalWave,
                buffer,
                clip,
                wave,
                mchorse.bbs_mod.camera.utils.TimeUtils.toSeconds(clip.tick.get()),
                mchorse.bbs_mod.camera.utils.TimeUtils.toSeconds(clip.offset.get()),
                mchorse.bbs_mod.camera.utils.TimeUtils.toSeconds(clip.duration.get()),
                clip.volume.get()
            );

            cir.cancel();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
