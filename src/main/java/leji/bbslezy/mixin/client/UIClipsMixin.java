package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.renderers.TimelineRulerRenderer;
import mchorse.bbs_mod.utils.clips.Clip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UIClips.class, remap = false)
public abstract class UIClipsMixin
{
    @Redirect(
        method = "renderCameraWork",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/film/clips/renderer/IUIClipRenderer;renderClip(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/film/UIClips;Lmchorse/bbs_mod/utils/clips/Clip;Lmchorse/bbs_mod/ui/utils/Area;ZZ)V"
        )
    )
    private void bbslezy$cullVerticalClip(IUIClipRenderer renderer, UIContext context, UIClips clips, Clip clip, Area clipArea, boolean selected, boolean current)
    {
        int rulerBottom = TimelineRulerRenderer.getRulerBottom(clips.area);
        if (clipArea.ey() < rulerBottom || clipArea.y > clips.area.ey())
        {
            return;
        }

        renderer.renderClip(context, clips, clip, clipArea, selected, current);
    }
}
