package leji.bbslezy.mixin.client;

import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIAudioClip;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIAudioClip.class, remap = false)
public abstract class UIAudioClipMixin<T extends AudioClip> extends UIClip<T>
{
    @Unique
    private UIKeyframeEditor bbslezy$keyframes;

    @Unique
    private UIButton bbslezy$editKeyframes;

    public UIAudioClipMixin(T clip, mchorse.bbs_mod.ui.film.IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Inject(method = "registerUI", at = @At("TAIL"))
    private void bbslezy$registerUI(CallbackInfo ci)
    {
        if (!((Object) this.getClass()).equals(UIAudioClip.class))
        {
            return;
        }

        try
        {
            this.bbslezy$keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
            this.bbslezy$keyframes.view.rulerRenderer((context) ->
            {
                UIReplaysEditor.renderRuler(context, this.bbslezy$keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get());
            });
            this.bbslezy$keyframes.view.duration(() -> this.clip.duration.get());
            this.bbslezy$keyframes.setUndoId("audio_keyframes");

            this.bbslezy$editKeyframes = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
            {
                this.editor.embedView(this.bbslezy$keyframes);
                this.bbslezy$keyframes.view.resetView();
                this.bbslezy$keyframes.view.getGraph().clearSelection();
            });
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
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

            if (this.bbslezy$editKeyframes != null)
            {
                this.panels.add(this.section(
                    L10n.lang("bbslezy.ui.audio.keyframes"),
                    this.bbslezy$editKeyframes
                ).tooltip(L10n.lang("bbslezy.ui.audio.keyframes.tooltip")));
            }

            this.panels.add(reverse);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    @Inject(method = "fillData", at = @At("TAIL"))
    private void bbslezy$fillData(CallbackInfo ci)
    {
        if (this.bbslezy$keyframes == null)
        {
            return;
        }

        try
        {
            this.bbslezy$keyframes.view.removeAllSheets();

            KeyframeChannel<Double> volumeChannel = LezyAudioReverse.getVolumeChannel(this.clip);

            if (volumeChannel != null)
            {
                this.bbslezy$keyframes.view.addSheet(new UIKeyframeSheet(
                    "volume",
                    IKey.constant("Volume"),
                    Colors.ACTIVE,
                    volumeChannel,
                    null
                ));
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
