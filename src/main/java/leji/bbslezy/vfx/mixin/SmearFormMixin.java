package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.forms.ISmearHolder;
import leji.bbslezy.vfx.forms.SmearProperties;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches {@link SmearProperties} to every {@link Form}.
 */
@Mixin(value = Form.class, remap = false)
public abstract class SmearFormMixin implements ISmearHolder
{
    @Unique
    private SmearProperties bbslezy$smearProps;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$initSmear(CallbackInfo ci)
    {
        /* LEGACY VALUE ID: "xavin_smear" predates rename; kept for saved scene compatibility. */
        this.bbslezy$smearProps = new SmearProperties("xavin_smear");
        ((ValueGroup) (Object) this).add(this.bbslezy$smearProps);
    }

    @Override
    public SmearProperties bbslezy$smear()
    {
        return this.bbslezy$smearProps;
    }
}
