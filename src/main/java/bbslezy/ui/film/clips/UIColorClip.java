package bbslezy.ui.film.clips;

import bbslezy.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class UIColorClip extends UIClip<ColorClip>
{
    public UIColor overlayColor;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UIColorClip(ColorClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.overlayColor = new UIColor((c) -> this.editor.editMultiple(this.clip.overlayColor, (value) ->
        {
            value.set(c);
        }));

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            UIReplaysEditor.renderRuler(context, this.keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get());
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("color_keyframes");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Overlay Color"), this.overlayColor));
        this.panels.add(this.section(IKey.raw("Keyframes"), this.edit));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.overlayColor.setColor(this.clip.overlayColor.get());
        this.keyframes.view.removeAllSheets();

        for (KeyframeChannel<?> channel : this.clip.channels)
        {
            int sheetColor = channel.getId().hashCode() & Colors.RGB;
            this.keyframes.view.addSheet(new UIKeyframeSheet(channel.getId(), IKey.constant(channel.getId()), sheetColor, channel, null));
        }
    }
}
