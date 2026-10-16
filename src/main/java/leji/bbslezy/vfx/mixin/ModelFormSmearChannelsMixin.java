package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.forms.IModelSmearChannels;
import leji.bbslezy.vfx.forms.ValueLinesPose;
import leji.bbslezy.vfx.forms.ValueSmearPose;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.pose.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the {@code smear_frames} and {@code motion_lines} per-bone keyframe channels to {@link ModelForm}.
 */
@Mixin(value = ModelForm.class, remap = false)
public abstract class ModelFormSmearChannelsMixin implements IModelSmearChannels
{
    @Unique
    private ValueSmearPose bbslezy$smear;

    @Unique
    private ValueLinesPose bbslezy$lines;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addChannels(CallbackInfo ci)
    {
        this.bbslezy$smear = new ValueSmearPose("smear_frames", new Pose());
        this.bbslezy$lines = new ValueLinesPose("motion_lines", new Pose());

        ((ValueGroup) (Object) this).add(this.bbslezy$smear);
        ((ValueGroup) (Object) this).add(this.bbslezy$lines);
    }

    @Override
    public BaseKeyframeFactoryValue<Pose> bbslezy$smearPose()
    {
        return this.bbslezy$smear;
    }

    @Override
    public BaseKeyframeFactoryValue<Pose> bbslezy$linesPose()
    {
        return this.bbslezy$lines;
    }
}
