package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import leji.bbslezy.vfx.forms.LezyWindForm;

/**
 * Editor panel for {@code bbslezy:wind}: the scan range + Scan button, the wind strength and visible-wind
 * densities, the direction handle (shown once scanned), and the master timeline.
 */
public class UIWindFormPanel extends UIFormPanel<LezyWindForm>
{
    public final UIButton scan;
    public final UITrackpad scanRadius;
    public final UITrackpad strength;
    public final UITrackpad windAngle;
    public final UITrackpad streaks;
    public final UITrackpad leaves;
    public final UITrackpad dust;
    public final UIColor color;

    public final UICirculate windType;
    public final UIButton preset;
    public final UITrackpad coreRadius;
    public final UITrackpad funnelFlare;
    public final UITrackpad funnelHeight;
    public final UITrackpad swirl;
    public final UITrackpad updraft;
    public final UITrackpad suction;
    public LezyVfxSection vortexSection;
    public final UITrackpad progress;
    public final UITrackpad opacity;
    public final UITrackpad duration;
    public final UIToggle sway;
    public final UITrackpad swayAmount;
    public final UIButton restore;

    public LezyVfxSection windSection;
    public LezyVfxSection directionSection;
    public LezyVfxSection swaySection;
    public LezyVfxSection timelineSection;

    public UIWindFormPanel(UIForm editor)
    {
        super(editor);

        this.scan = new UIButton(IKey.constant("Scan territory"), (b) ->
        {
            this.form.scanned.set(true);
            ((UIWindForm) this.editor).syncPointWidget();
            this.bbslezy$updateVisibility();
        });

        this.scanRadius = new UITrackpad((v) -> this.form.scanRadius.set(v.floatValue()));
        this.scanRadius.limit(1F, 512F).increment(1);
        this.strength = new UITrackpad((v) -> this.form.strength.set(v.floatValue()));
        this.strength.limit(0F, Float.POSITIVE_INFINITY).values(0.5F);
        this.windAngle = new UITrackpad((v) ->
        {
            if (this.form != null)
            {
                this.form.setAngle(v.floatValue());
                ((UIWindForm) this.editor).syncPointWidget();
            }
        });
        this.windAngle.limit(0F, 360F).values(5F);
        this.streaks = new UITrackpad((v) -> this.form.streaks.set(v.floatValue()));
        this.streaks.limit(0F, 1F).values(0.05F);
        this.leaves = new UITrackpad((v) -> this.form.leaves.set(v.floatValue()));
        this.leaves.limit(0F, 1F).values(0.05F);
        this.dust = new UITrackpad((v) -> this.form.dust.set(v.floatValue()));
        this.dust.limit(0F, 1F).values(0.05F);
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c)));

        this.windType = new UICirculate((b) ->
        {
            if (this.form != null)
            {
                this.form.windType.set(b.getValue());
                this.bbslezy$rebuild();
            }
        });
        this.windType.addLabel(IKey.constant("Directional"));
        this.windType.addLabel(IKey.constant("Storm"));
        this.windType.addLabel(IKey.constant("Tornado"));

        this.coreRadius = new UITrackpad((v) -> this.form.coreRadius.set(v.floatValue()));
        this.coreRadius.limit(0.5F, 128F).values(0.5F);
        this.funnelFlare = new UITrackpad((v) -> this.form.funnelFlare.set(v.floatValue()));
        this.funnelFlare.limit(0F, 8F).values(0.1F);
        this.funnelHeight = new UITrackpad((v) -> this.form.funnelHeight.set(v.floatValue()));
        this.funnelHeight.limit(2F, 320F).values(1F);
        this.swirl = new UITrackpad((v) -> this.form.swirl.set(v.floatValue()));
        this.swirl.limit(0F, 120F).values(0.5F);
        this.updraft = new UITrackpad((v) -> this.form.updraft.set(v.floatValue()));
        this.updraft.limit(0F, 60F).values(0.5F);
        this.suction = new UITrackpad((v) -> this.form.suction.set(v.floatValue()));
        this.suction.limit(0F, 60F).values(0.5F);

        this.preset = new UIButton(IKey.constant("Apply preset"), (b) ->
        {
            if (this.form != null)
            {
                this.form.applyPreset(this.form.windType.get());
                this.bbslezy$syncWidgets(this.form);
                this.bbslezy$rebuild();
            }
        });

        this.vortexSection = LezyVfxUI.section("Vortex (funnel)",
            UI.label(IKey.constant("Core radius / flare")), UI.row(this.coreRadius, this.funnelFlare),
            UI.label(IKey.constant("Funnel height")), this.funnelHeight,
            UI.label(IKey.constant("Swirl / updraft / suction")), UI.row(this.swirl, this.updraft, this.suction));

        this.progress = new UITrackpad((v) -> this.form.progress.set(v.floatValue()));
        this.progress.limit(0F, 1F).values(0.01F);
        this.opacity = new UITrackpad((v) -> this.form.opacity.set(v.floatValue()));
        this.opacity.limit(0F, 1F).values(0.01F);
        this.duration = new UITrackpad((v) -> this.form.duration.set(v.floatValue()));
        this.duration.limit(0.1F, 600F).values(0.1F);

        this.windSection = LezyVfxUI.section("Wind",
            this.scan,
            UI.label(IKey.constant("Scan range (blocks)")), this.scanRadius,
            UI.label(IKey.constant("Field type")), this.windType, this.preset,
            UI.label(IKey.constant("Strength")), this.strength,
            UI.label(IKey.constant("Streaks / leaves / dust")), UI.row(this.streaks, this.leaves, this.dust),
            UI.label(IKey.constant("Wind colour")), this.color);

        this.directionSection = LezyVfxUI.section("Direction",
            UI.label(IKey.constant("Wind direction angle (0°-360°)")), this.windAngle,
            ((UIWindForm) this.editor).pointGizmo());

        this.sway = new UIToggle(IKey.constant("Sway foliage (film only)"), (b) ->
        {
            if (this.form != null)
            {
                this.form.sway.set(b.getValue());
            }
        });
        this.swayAmount = new UITrackpad((v) -> this.form.swayAmount.set(v.floatValue()));
        this.swayAmount.limit(0F, 45F).values(0.5F);

        this.restore = new UIButton(IKey.constant("Restore foliage"), (b) ->
        {
            if (this.form != null)
            {
                this.form.sway.set(false);
                this.sway.setValue(false);
            }

            LezyWindFoliage.restore();
        });

        this.swaySection = LezyVfxUI.section("Foliage sway",
            this.sway,
            UI.label(IKey.constant("Sway amount (deg)")), this.swayAmount,
            this.restore);

        this.timelineSection = LezyVfxUI.section("Timeline",
            UI.label(IKey.constant("Progress / opacity")), UI.row(this.progress, this.opacity),
            UI.label(IKey.constant("Duration (s)")), this.duration);

        this.bbslezy$rebuild();
    }

    private void bbslezy$rebuild()
    {
        boolean scanned = this.form != null && this.form.scanned.get();

        this.options.removeAll();
        this.options.add(this.windSection);

        if (this.form != null && this.form.isVortex())
        {
            this.options.add(this.vortexSection);
        }

        if (scanned)
        {
            this.options.add(this.directionSection);
        }

        this.options.add(this.swaySection);
        this.options.add(this.timelineSection);
        this.options.resize();
    }

    private void bbslezy$updateVisibility()
    {
        this.bbslezy$rebuild();
    }

    @Override
    public void finishEdit()
    {
        super.finishEdit();
    }

    @Override
    public void startEdit(LezyWindForm form)
    {
        super.startEdit(form);

        this.bbslezy$syncWidgets(form);

        ((UIWindForm) this.editor).syncPointWidget();

        this.bbslezy$updateVisibility();
    }

    private void bbslezy$syncWidgets(LezyWindForm form)
    {
        this.scanRadius.setValue(form.scanRadius.get());
        this.strength.setValue(form.strength.get());
        this.windAngle.setValue(form.angle.get());
        this.streaks.setValue(form.streaks.get());
        this.leaves.setValue(form.leaves.get());
        this.dust.setValue(form.dust.get());
        this.color.setColor(form.color.get().getARGBColor());
        this.windType.setValue(form.windType.get());
        this.coreRadius.setValue(form.coreRadius.get());
        this.funnelFlare.setValue(form.funnelFlare.get());
        this.funnelHeight.setValue(form.funnelHeight.get());
        this.swirl.setValue(form.swirl.get());
        this.updraft.setValue(form.updraft.get());
        this.suction.setValue(form.suction.get());
        this.progress.setValue(form.progress.get());
        this.opacity.setValue(form.opacity.get());
        this.duration.setValue(form.duration.get());
        this.sway.setValue(form.sway.get());
        this.swayAmount.setValue(form.swayAmount.get());
    }
}
