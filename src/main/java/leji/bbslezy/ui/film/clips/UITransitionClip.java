package leji.bbslezy.ui.film.clips;

import leji.bbslezy.camera.clips.screen.TransitionClip;
import leji.bbslezy.camera.clips.screen.TransitionType;
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
import mchorse.bbs_mod.ui.framework.elements.buttons.UIChoiceButton;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.Arrays;

public class UITransitionClip extends UIClip<TransitionClip>
{
    public UIChoiceButton<TransitionType> type;
    public UIColor color;
    public UITrackpad intensity;
    public UIButton interp;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UITransitionClip(TransitionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.type = new UIChoiceButton<>(
            Arrays.asList(TransitionType.values()),
            (t) -> Icons.TIME,
            (t) -> IKey.raw(t.getLabel())
        ).callback((t) ->
        {
            this.editor.editMultiple(this.clip.type, (v) -> v.set(t.ordinal()));
        });
        this.type.tooltip(IKey.raw("Transition style (Fade Out, Fade In, Dip to Black, Flash, Crossfade)"));

        this.color = new UIColor((c) -> this.editor.editMultiple(this.clip.color, (v) -> v.set(Color.rgba(c))));
        this.color.tooltip(IKey.raw("Transition overlay color (default black, forced white for flash if black)"));

        this.intensity = this.trackpad(this.clip.intensity).limit(0F, 5F).values(0.05F, 0.2F, 1F);
        this.intensity.tooltip(IKey.raw("Transition opacity multiplier"));

        this.interp = new UIButton(UIKeys.CAMERA_PANELS_INTERPOLATION, (b) ->
        {
            this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.clip.interp));
        });
        this.interp.tooltip(new InterpolationTooltip(1F, 0.5F, () -> this.clip.interp));

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            UIReplaysEditor.renderRuler(context, this.keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get());
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("transition_keyframes");

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

        this.panels.add(this.section(IKey.raw("Transition Style"), this.type, this.color, this.intensity, this.interp));
        this.panels.add(this.section(IKey.raw("Animate with Keyframes"), this.edit).tooltip(
            IKey.raw("Optional: animate transition intensity and color over time")
        ));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        int typeIndex = this.clip.type.get();
        TransitionType[] types = TransitionType.values();
        if (typeIndex >= 0 && typeIndex < types.length)
        {
            this.type.setValue(types[typeIndex]);
        }

        this.color.setColor(this.clip.color.get().getARGBColor());
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
        if ("transition_keyframes".equals(data.getString("embed")))
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
            data.putString("embed", "transition_keyframes");
        }

        super.collectUndoData(data);
    }
}
