package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.film.clips.UIAudioClip;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIAudioClip.class, remap = false)
public abstract class UIAudioClipMixin<T extends AudioClip> extends UIClip<T>
{
    public UIAudioClipMixin(T clip, mchorse.bbs_mod.ui.film.IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Inject(method = "registerPanels", at = @At("TAIL"))
    private void bbslezy$registerPanels(CallbackInfo ci)
    {
        try
        {
            UIToggle reverse = new UIToggle(L10n.lang("bbslezy.ui.audio.reverse"), (b) ->
            {
                LezyAudioReverse.setEnabled(this.clip, b.getValue());
            });

            reverse.setValue(LezyAudioReverse.isEnabled(this.clip));
            reverse.tooltip(L10n.lang("bbslezy.ui.audio.reverse.tooltip"));

            this.panels.add(this.section(L10n.lang("bbslezy.ui.audio.reverse"), reverse));
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
