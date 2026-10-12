package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.controller.CameraWorkCameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.data.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import leji.bbslezy.vfx.camera.FollowCurveClip;

/**
 * Compose the follow-curve modifier at a clip's exclusive-end boundary in the editor preview.
 */
@Mixin(value = RunnerCameraController.class, remap = false)
public abstract class RunnerFollowCurveEndMixin
{
    @Inject(method = "applyEditedClipEnd", at = @At("TAIL"))
    private void bbslezy$composeFollowCurveAtEnd(int ticks, CallbackInfo ci)
    {
        CameraWorkCameraController self = (CameraWorkCameraController) (Object) this;
        CameraClipContext context = self.getContext();
        Position position = self.getPosition();

        if (context == null || context.clips == null || context.playing)
        {
            return;
        }

        for (FollowCurveClip clip : context.clips.getClips(FollowCurveClip.class))
        {
            if (clip.enabled.get() && clip.tick.get() + clip.duration.get() == ticks)
            {
                clip.applyLast(context, position);
            }
        }
    }
}
