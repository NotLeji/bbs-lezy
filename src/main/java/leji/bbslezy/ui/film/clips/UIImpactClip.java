package leji.bbslezy.ui.film.clips;

import leji.bbslezy.camera.clips.screen.ImpactClip;
import leji.bbslezy.camera.clips.screen.ImpactStyle;
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
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.Arrays;

public class UIImpactClip extends UIClip<ImpactClip>
{
    public UIChoiceButton<ImpactStyle> style;

    public UIToggle freezeEnabled;
    public UITrackpad freezeDuration;

    public UIToggle flashEnabled;
    public UIColor flashColor;
    public UITrackpad flashIntensity;
    public UITrackpad flashDuration;

    public UIToggle punchInEnabled;
    public UITrackpad punchInFov;
    public UITrackpad punchInDuration;

    public UIToggle shakeEnabled;
    public UITrackpad shakeIntensity;
    public UITrackpad shakeDuration;

    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UIImpactClip(ImpactClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.style = this.bind(new UIChoiceButton<>(
            Arrays.asList(ImpactStyle.values()),
            (s) -> Icons.FOUR_STAR,
            (s) -> IKey.raw(s.getLabel())
        ).callback((s) ->
        {
            this.editor.editMultiple(this.clip.style, (v) ->
            {
                this.clip.applyStyle(s);
            });
            this.fillData();
        }), () ->
        {
            int styleIndex = this.clip.style.get();
            ImpactStyle[] styles = ImpactStyle.values();
            if (styleIndex >= 0 && styleIndex < styles.length)
            {
                this.style.setValue(styles[styleIndex]);
            }
        });
        this.style.tooltip(IKey.raw("Preset impact styles (Anime Impact, Hard Hit, Explosion, Dramatic)"));

        /* Freeze */
        this.freezeEnabled = this.toggle(IKey.raw("Freeze Frame"), this.clip.freezeEnabled);
        this.freezeDuration = this.trackpad(this.clip.freezeDuration).limit(0, 100).values(1, 2, 5);
        this.freezeDuration.tooltip(IKey.raw("Freeze duration in ticks"));

        /* Flash */
        this.flashEnabled = this.toggle(IKey.raw("Screen Flash"), this.clip.flashEnabled);
        this.flashColor = this.bind(new UIColor((c) -> this.editor.editMultiple(this.clip.flashColor, (v) -> v.set(Color.rgba(c)))), () -> this.flashColor.setColor(this.clip.flashColor.get().getARGBColor()));
        this.flashIntensity = this.trackpad(this.clip.flashIntensity).limit(0F, 5F).values(0.05F, 0.2F, 1F);
        this.flashIntensity.tooltip(IKey.raw("Flash peak opacity"));
        this.flashDuration = this.trackpad(this.clip.flashDuration).limit(1, 100).values(1, 2, 5);
        this.flashDuration.tooltip(IKey.raw("Flash fade duration in ticks"));

        /* Punch-in */
        this.punchInEnabled = this.toggle(IKey.raw("FOV Punch-In"), this.clip.punchInEnabled);
        this.punchInFov = this.trackpad(this.clip.punchInFov).limit(0F, 90F).values(1F, 5F, 10F);
        this.punchInFov.tooltip(IKey.raw("FOV reduction amount (zoom in)"));
        this.punchInDuration = this.trackpad(this.clip.punchInDuration).limit(1, 100).values(1, 2, 5);
        this.punchInDuration.tooltip(IKey.raw("Punch-in decay duration in ticks"));

        /* Shake */
        this.shakeEnabled = this.toggle(IKey.raw("Camera Shake"), this.clip.shakeEnabled);
        this.shakeIntensity = this.trackpad(this.clip.shakeIntensity).limit(0F, 10F).values(0.1F, 0.5F, 1F);
        this.shakeIntensity.tooltip(IKey.raw("Impact shake peak amplitude"));
        this.shakeDuration = this.trackpad(this.clip.shakeDuration).limit(1, 100).values(1, 2, 5);
        this.shakeDuration.tooltip(IKey.raw("Shake decay duration in ticks"));

        /* Keyframes */
        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            UIReplaysEditor.renderRuler(context, this.keyframes.view, (UIClipsPanel) this.editor, (Clips) this.clip.getParent(), this.clip.tick.get());
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("impact_keyframes");

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

        this.panels.add(this.section(IKey.raw("Presets"), this.style));
        this.panels.add(this.section(IKey.raw("Freeze Frame"),
            this.freezeEnabled,
            UI.row(UIConstants.MARGIN, 0, 20, this.freezeDuration)
        ));
        this.panels.add(this.section(IKey.raw("Screen Flash"),
            this.flashEnabled,
            this.flashColor,
            UI.row(UIConstants.MARGIN, 0, 20, this.flashIntensity, this.flashDuration)
        ));
        this.panels.add(this.section(IKey.raw("Punch-In FOV"),
            this.punchInEnabled,
            UI.row(UIConstants.MARGIN, 0, 20, this.punchInFov, this.punchInDuration)
        ));
        this.panels.add(this.section(IKey.raw("Camera Shake Impulse"),
            this.shakeEnabled,
            UI.row(UIConstants.MARGIN, 0, 20, this.shakeIntensity, this.shakeDuration)
        ));
        this.panels.add(this.section(IKey.raw("Animate with Keyframes"), this.edit).tooltip(
            IKey.raw("Optional: animate overall impact intensity over time")
        ));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        int styleIndex = this.clip.style.get();
        ImpactStyle[] styles = ImpactStyle.values();
        if (styleIndex >= 0 && styleIndex < styles.length)
        {
            this.style.setValue(styles[styleIndex]);
        }

        this.flashColor.setColor(this.clip.flashColor.get().getARGBColor());
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
        if ("impact_keyframes".equals(data.getString("embed")))
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
            data.putString("embed", "impact_keyframes");
        }

        super.collectUndoData(data);
    }
}
