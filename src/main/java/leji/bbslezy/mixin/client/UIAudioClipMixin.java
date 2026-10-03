package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
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
        /* Guard against subclasses like UIVideoClip appending reverse option again */
        if (!((Object) this.getClass()).equals(UIAudioClip.class))
        {
            return;
        }

        try
        {
            ValueBoolean revVal = (ValueBoolean) ((mchorse.bbs_mod.settings.values.core.ValueGroup) (Object) this.clip).get(LezyAudioReverse.KEY_REVERSE);
            UIToggle reverse;

            if (revVal != null)
            {
                reverse = this.toggle(L10n.lang("bbslezy.ui.audio.reverse"), revVal);
            }
            else
            {
                reverse = new UIToggle(L10n.lang("bbslezy.ui.audio.reverse"), (b) ->
                {
                    LezyAudioReverse.setEnabled(this.clip, b.getValue());
                });
                this.bind(reverse, () -> reverse.setValue(LezyAudioReverse.isEnabled(this.clip)));
            }

            reverse.tooltip(L10n.lang("bbslezy.ui.audio.reverse.tooltip"));
            this.panels.add(reverse);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
