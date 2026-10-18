package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.types.ToWAVImporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Takes {@link ToWAVImporter} out of the chain: it claims .mp3 as well as .mp4 (and is registered
 * before the addon's importers, so it would win), and it rewrites whatever it gets into mono WAV.
 *
 * <p>For an audio file that re-encodes what {@code LezyCopyAudioImporter} would keep verbatim; for
 * a video it throws the picture away — a dropped clip became a .wav, which is exactly what it
 * should not do. Nothing needs it: audio is copied as-is, videos are copied as-is into the video
 * folder, and a video's audio track is decoded on demand by {@code AudioReader.readVideoAudio}.</p>
 */
@Mixin(value = ToWAVImporter.class, remap = false)
public abstract class ToWAVImporterMixin
{
    @Inject(method = "canImport", at = @At("HEAD"), cancellable = true)
    private void bbslezy$noWavConversion(ImporterContext context, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }
}
