package bbslezy.mixin.client;

import mchorse.bbs_mod.film.VideoExportSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VideoExportSession.class, remap = false)
public abstract class VideoExportSessionMixin
{
    /**
     * Prevent warmup from aborting prematurely during F6 Record & Replay.
     */
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/film/VideoExportSession;shouldAbortWarmup()Z"
        )
    )
    private boolean bbslezy$ignoreShouldAbortWarmup(VideoExportSession session)
    {
        return false;
    }
}
