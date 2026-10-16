package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.forms.CurveForm;
import leji.bbslezy.vfx.forms.DestructionBoxForm;
import leji.bbslezy.vfx.forms.IFormSmear;
import leji.bbslezy.vfx.forms.ValueFormSmearPose;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.pose.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds ONE whole-form smear keyframe channel ({@code smear}) to non-model forms.
 */
@Mixin(value = Form.class, remap = false)
public abstract class WholeFormSmearMixin implements IFormSmear
{
    @Unique
    private ValueFormSmearPose bbslezy$formSmear;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addFormSmear(CallbackInfo ci)
    {
        Object self = this;

        if (self instanceof ModelForm || self instanceof CurveForm || self instanceof DestructionBoxForm
            || self instanceof ParticleForm || self instanceof VanillaParticleForm
            || self instanceof TrailForm)
        {
            return;
        }

        Pose pose = new Pose();
        pose.getOrCreate(IFormSmear.SENTINEL);

        this.bbslezy$formSmear = new ValueFormSmearPose("smear", pose);
        ((ValueGroup) self).add(this.bbslezy$formSmear);
    }

    @Override
    public Pose bbslezy$formSmearPose()
    {
        return this.bbslezy$formSmear == null ? null : this.bbslezy$formSmear.get();
    }
}
