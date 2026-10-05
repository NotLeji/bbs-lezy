package leji.bbslezy.ui.film.clips;

import leji.bbslezy.camera.clips.screen.HalftoneClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class UIHalftoneClip extends UIClip<HalftoneClip>
{
    public UICirculate mode;
    public UIColor inkColor;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UIHalftoneClip(HalftoneClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.mode = new UICirculate((b) -> this.editor.editMultiple(this.clip.mode, (value) ->
        {
            value.set(b.getValue());
        }));
        this.mode.addLabel(IKey.raw("B&W"));
        this.mode.addLabel(IKey.raw("Color"));
        this.mode.tooltip(IKey.raw("Halftone rendering mode: B&W (custom ink on paper) or Color (dots keep original pixel color)"));

        this.inkColor = new UIColor((c) -> this.editor.editMultiple(this.clip.inkColor, (value) ->
        {
            value.set(c);
        }));
        this.inkColor.tooltip(IKey.raw("Ink color for B&W halftone screen-tone (default black)"));

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            UIReplaysEditor.renderRuler(context, this.keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get());
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("halftone_keyframes");

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

        this.panels.add(this.section(IKey.raw("Mode"), this.mode));
        this.panels.add(this.section(IKey.raw("Ink Color"), this.inkColor));
        this.panels.add(this.section(IKey.raw("Keyframes (Cells, Highlight Thresh, Dot Softness, Angle)"), this.edit).tooltip(
            IKey.raw("Animate halftone cells density, highlight threshold, dot softness, and grid angle")
        ));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.mode.setValue(this.clip.mode.get());
        this.inkColor.setColor(this.clip.inkColor.get());
        this.keyframes.view.removeAllSheets();

        for (KeyframeChannel<?> channel : this.clip.channels)
        {
            int sheetColor = channel.getId().hashCode() & Colors.RGB;
            this.keyframes.view.addSheet(new UIKeyframeSheet(channel.getId(), IKey.constant(channel.getId()), sheetColor, channel, null));
        }
    }

    @Override
    public void applyUndoData(MapType data)
    {
        if ("halftone_keyframes".equals(data.getString("embed")))
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
        }

        super.applyUndoData(data);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        if (this.keyframes.hasParent())
        {
            data.putString("embed", "halftone_keyframes");
        }

        super.collectUndoData(data);
    }
}
