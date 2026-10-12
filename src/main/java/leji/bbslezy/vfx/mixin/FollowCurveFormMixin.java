package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the animatable follow-curve travel {@code follow_offset} (0..1 along the curve) to every
 * {@link Form}.
 */
@Mixin(value = Form.class, remap = false)
public abstract class FollowCurveFormMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addFollowCurve(CallbackInfo ci)
    {
        ValueGroup self = (ValueGroup) (Object) this;

        self.add(new ValueFloat("follow_offset", 0F, 0F, 1F));
    }
}
