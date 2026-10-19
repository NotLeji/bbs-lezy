package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.ISmearPoseEditor;
import leji.bbslezy.vfx.client.LezyVfxSection;
import leji.bbslezy.vfx.client.LezyVfxUI;
import leji.bbslezy.vfx.forms.ISmearBone;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adds per-bone smear and motion line controls to UIPoseEditor.
 */
@Mixin(value = UIPoseEditor.class, remap = false)
public abstract class UIPoseEditorSmearMixin implements ISmearPoseEditor
{
    @Shadow
    private Pose pose;

    @Shadow
    private void forEachSelectedPose(Consumer<? super PoseTransform> consumer)
    {
        throw new AssertionError();
    }

    @Unique private UITrackpad bbslezy$smearX;
    @Unique private UITrackpad bbslezy$smearY;
    @Unique private UITrackpad bbslezy$smearZ;
    @Unique private UITrackpad bbslezy$smearCount;
    @Unique private UITrackpad bbslezy$smearFalloff;
    @Unique private UITrackpad bbslezy$smearDissolve;
    @Unique private UIToggle bbslezy$smearManual;
    @Unique private UIToggle bbslezy$smearArc;
    @Unique private UITrackpad bbslezy$smearTime;
    @Unique private UITrackpad bbslezy$smearStretch;
    @Unique private UITrackpad bbslezy$smearDensity;
    @Unique private UITrackpad bbslezy$smearOpacity;
    @Unique private UIToggle bbslezy$linesAuto;
    @Unique private UIToggle bbslezy$linesManual;
    @Unique private UITrackpad bbslezy$smearLinesCount;
    @Unique private UITrackpad bbslezy$smearLinesWidth;
    @Unique private UITrackpad bbslezy$smearLinesSpread;
    @Unique private UITrackpad bbslezy$smearLinesReach;
    @Unique private UITrackpad bbslezy$smearLinesLength;
    @Unique private UIToggle bbslezy$smearLinesBlunt;
    @Unique private UIToggle bbslezy$smearLinesTexture;

    @Unique private UIElement bbslezy$linesRow1;
    @Unique private UIElement bbslezy$linesRow2;
    @Unique private UIElement bbslezy$linesAutoRow;
    @Unique private boolean bbslezy$linesAutoOpen;
    @Unique private boolean bbslezy$linesManualOpen;

    @Unique private UIElement bbslezy$smearBlock;
    @Unique private UIElement bbslezy$linesBlock;

    @Unique private LezyVfxSection bbslezy$smearSection;
    @Unique private LezyVfxSection bbslezy$linesSection;

    @Unique private UIElement bbslezy$manualRow;
    @Unique private UIElement bbslezy$manualRow2;
    @Unique private UIElement bbslezy$autoRow1;
    @Unique private UIElement bbslezy$autoRow2;
    @Unique private UIElement bbslezy$commonRow;

    @Unique private boolean bbslezy$manualOpen;
    @Unique private boolean bbslezy$autoOpen;

    @Override
    public UIElement bbslezy$smearBlock()
    {
        return this.bbslezy$smearSection;
    }

    @Override
    public UIElement bbslezy$linesBlock()
    {
        return this.bbslezy$linesSection;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addSmear(CallbackInfo ci)
    {
        this.bbslezy$smearX = new UITrackpad((v) -> this.bbslezy$apply(0, v.floatValue())).values(0.05F);
        this.bbslezy$smearY = new UITrackpad((v) -> this.bbslezy$apply(1, v.floatValue())).values(0.05F);
        this.bbslezy$smearZ = new UITrackpad((v) -> this.bbslezy$apply(2, v.floatValue())).values(0.05F);

        this.bbslezy$smearCount = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearCount(v.floatValue())));
        this.bbslezy$smearCount.limit(0F, 32F).increment(1).tooltip(IKey.constant("Smear copies (0 = default 4)"));
        this.bbslezy$smearFalloff = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearFalloff(v.floatValue())));
        this.bbslezy$smearFalloff.limit(0F, 1F).values(0.01F).tooltip(IKey.constant("Fade toward the tail (0 = default)"));
        this.bbslezy$smearDissolve = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearDissolve(v.floatValue())));
        this.bbslezy$smearDissolve.limit(0F, 1F).values(0.01F).tooltip(IKey.constant("Dissolve: noise holes eat the trailing copies (shared)"));

        this.bbslezy$smearManual = new UIToggle(IKey.constant("Manual"), (b) -> this.bbslezy$selectMode(false, b.getValue()));
        this.bbslezy$smearManual.tooltip(IKey.constant("Straight smear along a manual X/Y/Z vector"));

        this.bbslezy$smearArc = new UIToggle(IKey.constant("Auto"), (b) -> this.bbslezy$selectMode(true, b.getValue()));
        this.bbslezy$smearArc.tooltip(IKey.constant("Follow the bone's real movement arc (time-rewound), not a manual vector"));
        this.bbslezy$smearTime = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearTime(v.floatValue())));
        this.bbslezy$smearTime.limit(0F, 40F).values(0.5F).tooltip(IKey.constant("Arc trail length in ticks (0 = default 4)"));
        this.bbslezy$smearStretch = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearStretch(v.floatValue())));
        this.bbslezy$smearStretch.limit(0F, 4F).values(0.05F).tooltip(IKey.constant("Copy stretch along the bone length + squash (0 = off; keep 0 for a head turn)"));
        this.bbslezy$smearDensity = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearDensity(v.floatValue())));
        this.bbslezy$smearDensity.limit(0F, 48F).increment(1).tooltip(IKey.constant("Overlap density: faint in-between copies merging into a continuous smear (0 = default 16)"));
        this.bbslezy$smearOpacity = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearOpacity(v.floatValue())));
        this.bbslezy$smearOpacity.limit(0F, 1F).values(0.01F).tooltip(IKey.constant("Opacity of each overlap copy (0 = default 0.14)"));

        this.bbslezy$manualRow = UI.row(this.bbslezy$smearX, this.bbslezy$smearY, this.bbslezy$smearZ);
        this.bbslezy$manualRow2 = UI.row(this.bbslezy$smearCount, this.bbslezy$smearFalloff);
        this.bbslezy$autoRow1 = UI.row(this.bbslezy$smearTime, this.bbslezy$smearStretch);
        this.bbslezy$autoRow2 = UI.row(this.bbslezy$smearDensity, this.bbslezy$smearOpacity);
        this.bbslezy$commonRow = UI.row(this.bbslezy$smearDissolve);

        this.bbslezy$linesAuto = new UIToggle(IKey.constant("Auto"), (b) -> this.bbslezy$selectLinesMode(true, b.getValue()));
        this.bbslezy$linesAuto.tooltip(IKey.constant("Lines follow the bone's real motion arc (volumetric, speed-driven)"));
        this.bbslezy$linesManual = new UIToggle(IKey.constant("Manual"), (b) -> this.bbslezy$selectLinesMode(false, b.getValue()));
        this.bbslezy$linesManual.tooltip(IKey.constant("A fan of straight streaks you aim by hand (the channel rotation)"));

        this.bbslezy$smearLinesCount = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesCount(v.floatValue())));
        this.bbslezy$smearLinesCount.limit(0F, 64F).increment(1).tooltip(IKey.constant("Lines count (0 = default)"));
        this.bbslezy$smearLinesWidth = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesWidth(v.floatValue())));
        this.bbslezy$smearLinesWidth.limit(0F, Float.POSITIVE_INFINITY).values(0.001F).tooltip(IKey.constant("Lines thickness (0 = default)"));
        this.bbslezy$smearLinesSpread = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesSpread(v.floatValue())));
        this.bbslezy$smearLinesSpread.limit(0F, Float.POSITIVE_INFINITY).values(0.01F).tooltip(IKey.constant("Auto: volume radius around the limb. Manual: fan spread. (0 = default)"));
        this.bbslezy$smearLinesLength = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesOffset(s.bbslezy$smearLinesOffsetX(), v.floatValue(), s.bbslezy$smearLinesOffsetZ())));
        this.bbslezy$smearLinesLength.limit(0F, Float.POSITIVE_INFINITY).values(0.05F).tooltip(IKey.constant("Length multiplier (0 = default)"));
        this.bbslezy$smearLinesReach = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesOffset(v.floatValue(), s.bbslezy$smearLinesOffsetY(), s.bbslezy$smearLinesOffsetZ())));
        this.bbslezy$smearLinesReach.limit(0F, Float.POSITIVE_INFINITY).values(0.02F).tooltip(IKey.constant("Reach: push the line origins down the limb onto the hand (0 = auto)"));
        this.bbslezy$smearLinesBlunt = new UIToggle(IKey.constant("Blunt ends"), (b) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesOffset(s.bbslezy$smearLinesOffsetX(), s.bbslezy$smearLinesOffsetY(), b.getValue() ? 1F : 0F)));
        this.bbslezy$smearLinesBlunt.tooltip(IKey.constant("Uniform width instead of tapering the ends to a point"));
        this.bbslezy$smearLinesTexture = new UIToggle(IKey.constant("Texture color"), (b) -> this.bbslezy$forEach((s) -> s.bbslezy$setSmearLinesTexture(b.getValue() ? 1F : 0F)));
        this.bbslezy$smearLinesTexture.tooltip(IKey.constant("Tint lines with the actor's texture instead of the Color picker"));

        this.bbslezy$smearBlock = UI.column();
        this.bbslezy$rebuildSmear();
        this.bbslezy$smearSection = LezyVfxUI.section(IKey.constant("Smear"), this.bbslezy$smearBlock);

        this.bbslezy$linesRow1 = UI.row(this.bbslezy$smearLinesCount, this.bbslezy$smearLinesWidth, this.bbslezy$smearLinesSpread);
        this.bbslezy$linesRow2 = UI.row(this.bbslezy$smearLinesLength, this.bbslezy$smearLinesTexture);
        this.bbslezy$linesAutoRow = UI.row(this.bbslezy$smearLinesReach, this.bbslezy$smearLinesBlunt);

        this.bbslezy$linesBlock = UI.column();
        this.bbslezy$rebuildLines();
        this.bbslezy$linesSection = LezyVfxUI.section(IKey.constant("Motion lines"), this.bbslezy$linesBlock);
    }

    @Unique
    private void bbslezy$rebuildLines()
    {
        this.bbslezy$linesBlock.removeAll();
        this.bbslezy$linesBlock.add(this.bbslezy$linesAuto.marginTop(6));
        this.bbslezy$linesBlock.add(this.bbslezy$linesManual);

        if (this.bbslezy$linesAutoOpen || this.bbslezy$linesManualOpen)
        {
            this.bbslezy$linesBlock.add(this.bbslezy$linesRow1, this.bbslezy$linesRow2);

            if (this.bbslezy$linesAutoOpen)
            {
                this.bbslezy$linesBlock.add(this.bbslezy$linesAutoRow);
            }
        }
    }

    @Unique
    private void bbslezy$selectLinesMode(boolean auto, boolean on)
    {
        if (auto)
        {
            this.bbslezy$linesAutoOpen = on;

            if (on)
            {
                this.bbslezy$linesManualOpen = false;
            }
        }
        else
        {
            this.bbslezy$linesManualOpen = on;

            if (on)
            {
                this.bbslezy$linesAutoOpen = false;
            }
        }

        float mode = this.bbslezy$linesAutoOpen ? 1F : (this.bbslezy$linesManualOpen ? 2F : 0F);
        this.bbslezy$forEach((s) -> s.bbslezy$setSmearLines(mode));

        this.bbslezy$linesAuto.setValue(this.bbslezy$linesAutoOpen);
        this.bbslezy$linesManual.setValue(this.bbslezy$linesManualOpen);
        this.bbslezy$rebuildLines();
        this.bbslezy$reflowFrom(this.bbslezy$linesBlock);
    }

    @Unique
    private void bbslezy$rebuildSmear()
    {
        this.bbslezy$smearBlock.removeAll();
        this.bbslezy$smearBlock.add(this.bbslezy$smearManual.marginTop(6));

        if (this.bbslezy$manualOpen)
        {
            this.bbslezy$smearBlock.add(this.bbslezy$manualRow, this.bbslezy$manualRow2);
        }

        this.bbslezy$smearBlock.add(this.bbslezy$smearArc);

        if (this.bbslezy$autoOpen)
        {
            this.bbslezy$smearBlock.add(this.bbslezy$autoRow1, this.bbslezy$autoRow2);
        }

        this.bbslezy$smearBlock.add(this.bbslezy$commonRow);
    }

    @Unique
    private void bbslezy$selectMode(boolean auto, boolean on)
    {
        if (auto)
        {
            this.bbslezy$autoOpen = on;

            if (on)
            {
                this.bbslezy$manualOpen = false;
            }

            this.bbslezy$forEach((s) ->
            {
                s.bbslezy$setSmearArc(on ? 1F : 0F);

                if (on)
                {
                    s.bbslezy$setSmearManual(0F);
                }
            });
        }
        else
        {
            this.bbslezy$manualOpen = on;

            if (on)
            {
                this.bbslezy$autoOpen = false;
            }

            this.bbslezy$forEach((s) ->
            {
                s.bbslezy$setSmearManual(on ? 1F : 0F);

                if (on)
                {
                    s.bbslezy$setSmearArc(0F);
                }
            });
        }

        this.bbslezy$smearManual.setValue(this.bbslezy$manualOpen);
        this.bbslezy$smearArc.setValue(this.bbslezy$autoOpen);
        this.bbslezy$rebuildSmear();
        this.bbslezy$reflow();
    }

    @Unique
    private void bbslezy$reflow()
    {
        this.bbslezy$reflowFrom(this.bbslezy$smearBlock);
    }

    @Unique
    private void bbslezy$reflowFrom(UIElement start)
    {
        UIElement root = start;

        while (root.getParent() != null)
        {
            root = root.getParent();
        }

        root.resize();
    }

    @Unique
    private void bbslezy$forEach(Consumer<ISmearBone> action)
    {
        this.forEachSelectedPose((pt) ->
        {
            if (pt instanceof ISmearBone bone)
            {
                action.accept(bone);
            }
        });
    }

    @Unique
    private void bbslezy$apply(int axis, float value)
    {
        this.bbslezy$forEach((s) ->
        {
            float x = s.bbslezy$smearX();
            float y = s.bbslezy$smearY();
            float z = s.bbslezy$smearZ();

            if (axis == 0)
            {
                x = value;
            }
            else if (axis == 1)
            {
                y = value;
            }
            else
            {
                z = value;
            }

            s.bbslezy$setSmear(x, y, z);
        });
    }

    @Inject(method = "pickBones", at = @At("TAIL"))
    private void bbslezy$sync(List<String> bones, CallbackInfo ci)
    {
        if (bones.isEmpty() || this.pose == null)
        {
            this.bbslezy$smearX.setValue(0F);
            this.bbslezy$smearY.setValue(0F);
            this.bbslezy$smearZ.setValue(0F);
            this.bbslezy$smearCount.setValue(0F);
            this.bbslezy$smearFalloff.setValue(0F);
            this.bbslezy$smearDissolve.setValue(0F);
            this.bbslezy$smearTime.setValue(0F);
            this.bbslezy$smearStretch.setValue(0F);
            this.bbslezy$smearDensity.setValue(0F);
            this.bbslezy$smearOpacity.setValue(0F);
            this.bbslezy$smearLinesCount.setValue(0F);
            this.bbslezy$smearLinesWidth.setValue(0F);
            this.bbslezy$smearLinesSpread.setValue(0F);
            this.bbslezy$smearLinesReach.setValue(0F);
            this.bbslezy$smearLinesLength.setValue(0F);
            this.bbslezy$smearLinesBlunt.setValue(false);
            this.bbslezy$smearLinesTexture.setValue(false);
            this.bbslezy$manualOpen = false;
            this.bbslezy$autoOpen = false;
            this.bbslezy$smearManual.setValue(false);
            this.bbslezy$smearArc.setValue(false);
            this.bbslezy$rebuildSmear();
            this.bbslezy$linesAutoOpen = false;
            this.bbslezy$linesManualOpen = false;
            this.bbslezy$linesAuto.setValue(false);
            this.bbslezy$linesManual.setValue(false);
            this.bbslezy$rebuildLines();

            return;
        }

        ISmearBone s = (ISmearBone) this.pose.getOrCreate(bones.get(0));

        this.bbslezy$smearX.setValue(s.bbslezy$smearX());
        this.bbslezy$smearY.setValue(s.bbslezy$smearY());
        this.bbslezy$smearZ.setValue(s.bbslezy$smearZ());
        this.bbslezy$smearCount.setValue(s.bbslezy$smearCount());
        this.bbslezy$smearFalloff.setValue(s.bbslezy$smearFalloff());
        this.bbslezy$smearDissolve.setValue(s.bbslezy$smearDissolve());
        this.bbslezy$smearTime.setValue(s.bbslezy$smearTime());
        this.bbslezy$smearStretch.setValue(s.bbslezy$smearStretch());
        this.bbslezy$smearDensity.setValue(s.bbslezy$smearDensity());
        this.bbslezy$smearOpacity.setValue(s.bbslezy$smearOpacity());
        this.bbslezy$smearLinesCount.setValue(s.bbslezy$smearLinesCount());
        this.bbslezy$smearLinesWidth.setValue(s.bbslezy$smearLinesWidth());
        this.bbslezy$smearLinesSpread.setValue(s.bbslezy$smearLinesSpread());
        this.bbslezy$smearLinesReach.setValue(s.bbslezy$smearLinesOffsetX());
        this.bbslezy$smearLinesLength.setValue(s.bbslezy$smearLinesOffsetY());
        this.bbslezy$smearLinesBlunt.setValue(s.bbslezy$smearLinesOffsetZ() > 0F);
        this.bbslezy$smearLinesTexture.setValue(s.bbslezy$smearLinesTexture() > 0F);

        this.bbslezy$autoOpen = s.bbslezy$smearArc() > 0F;
        this.bbslezy$manualOpen = !this.bbslezy$autoOpen && s.bbslezy$smearManual() > 0F;
        this.bbslezy$smearManual.setValue(this.bbslezy$manualOpen);
        this.bbslezy$smearArc.setValue(this.bbslezy$autoOpen);
        this.bbslezy$rebuildSmear();

        this.bbslezy$linesAutoOpen = Math.abs(s.bbslezy$smearLines() - 1F) < 0.5F;
        this.bbslezy$linesManualOpen = s.bbslezy$smearLines() >= 1.5F;
        this.bbslezy$linesAuto.setValue(this.bbslezy$linesAutoOpen);
        this.bbslezy$linesManual.setValue(this.bbslezy$linesManualOpen);
        this.bbslezy$rebuildLines();
    }
}
