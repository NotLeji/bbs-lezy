package leji.bbslezy.ui.film.clips;

import leji.bbslezy.camera.clips.modifiers.ProceduralShakeClip;
import leji.bbslezy.camera.clips.modifiers.ShakePreset;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.clips.widgets.UIBitToggle;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIChoiceButton;
import java.util.Arrays;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIProceduralShakeClip extends UIClip<ProceduralShakeClip>
{
    public UITrackpad frequency;
    public UITrackpad octaves;
    public UITrackpad seed;
    public UITrackpad intensity;

    public UITrackpad posX;
    public UITrackpad posY;
    public UITrackpad posZ;
    public UITrackpad rotYaw;
    public UITrackpad rotPitch;
    public UITrackpad rotRoll;
    public UITrackpad rotFov;

    public UIBitToggle active;
    public UIChoiceButton<ShakePreset> presetChoice;

    public UIProceduralShakeClip(ProceduralShakeClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.frequency = this.trackpad(this.clip.frequency).limit(0.01F, 20F).values(0.05F, 0.2F, 1F);
        this.frequency.tooltip(IKey.raw("Shake speed / frequency"));

        this.octaves = this.trackpad(this.clip.octaves).limit(1, 6).values(1, 1, 1);
        this.octaves.tooltip(IKey.raw("Noise detail octaves (1 = smooth, 6 = fine jitter)"));

        this.seed = this.trackpad(this.clip.seed);
        this.seed.tooltip(IKey.raw("Random seed"));

        this.intensity = this.trackpad(this.clip.intensity).limit(0F, 10F).values(0.05F, 0.2F, 1F);
        this.intensity.tooltip(IKey.raw("Overall shake amplitude multiplier"));

        this.posX = this.trackpad(this.clip.posX).values(0.01F, 0.05F, 0.1F);
        this.posX.tooltip(IKey.raw("X translation amplitude"));
        this.posY = this.trackpad(this.clip.posY).values(0.01F, 0.05F, 0.1F);
        this.posY.tooltip(IKey.raw("Y translation amplitude"));
        this.posZ = this.trackpad(this.clip.posZ).values(0.01F, 0.05F, 0.1F);
        this.posZ.tooltip(IKey.raw("Z translation amplitude"));

        this.rotYaw = this.trackpad(this.clip.rotYaw).values(0.1F, 0.5F, 1F);
        this.rotYaw.tooltip(IKey.raw("Yaw rotation amplitude (degrees)"));
        this.rotPitch = this.trackpad(this.clip.rotPitch).values(0.1F, 0.5F, 1F);
        this.rotPitch.tooltip(IKey.raw("Pitch rotation amplitude (degrees)"));
        this.rotRoll = this.trackpad(this.clip.rotRoll).values(0.1F, 0.5F, 1F);
        this.rotRoll.tooltip(IKey.raw("Roll rotation amplitude (degrees)"));
        this.rotFov = this.trackpad(this.clip.rotFov).values(0.1F, 0.5F, 1F);
        this.rotFov.tooltip(IKey.raw("FOV oscillation amplitude"));

        this.active = this.bind(new UIBitToggle((value) -> this.clip.active.set(value)).all(), () -> this.active.setValue(this.clip.active.get()));
        this.presetChoice = this.bind(new UIChoiceButton<>(
            Arrays.asList(ShakePreset.values()),
            (p) -> Icons.EXCHANGE,
            (p) -> IKey.raw(p.getLabel())
        ).callback((p) ->
        {
            this.editor.editMultiple(this.clip.preset, (v) ->
            {
                this.clip.applyPreset(p.ordinal());
            });
            this.fillData();
        }), () ->
        {
            int presetIndex = this.clip.preset.get();
            ShakePreset[] presets = ShakePreset.values();
            if (presetIndex >= 0 && presetIndex < presets.length)
            {
                this.presetChoice.setValue(presets[presetIndex]);
            }
        });
        this.presetChoice.tooltip(IKey.raw("Apply shake presets (Gentle, Action, Explosion)"));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Presets"), this.presetChoice));
        this.panels.add(this.section(IKey.raw("Noise Parameters"),
            UI.row(UIConstants.MARGIN, 0, 20, this.frequency, this.intensity),
            UI.row(UIConstants.MARGIN, 0, 20, this.octaves, this.seed)
        ));
        this.panels.add(this.section(IKey.raw("Position Amplitude"),
            UI.row(UIConstants.MARGIN, 0, 20, this.posX, this.posY, this.posZ)
        ));
        this.panels.add(this.section(IKey.raw("Angle & FOV Amplitude"),
            UI.row(UIConstants.MARGIN, 0, 20, this.rotYaw, this.rotPitch),
            UI.row(UIConstants.MARGIN, 0, 20, this.rotRoll, this.rotFov)
        ));
        this.panels.add(this.section(IKey.raw("Active Axes"), this.active));
    }
    @Override
    public void fillData()
    {
        super.fillData();

        int presetIndex = this.clip.preset.get();
        ShakePreset[] presets = ShakePreset.values();
        if (presetIndex >= 0 && presetIndex < presets.length)
        {
            this.presetChoice.setValue(presets[presetIndex]);
        }
    }

}
