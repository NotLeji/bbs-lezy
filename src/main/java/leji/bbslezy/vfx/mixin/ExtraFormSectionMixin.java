package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.ExtraFormSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import leji.bbslezy.vfx.forms.DestructionBoxForm;
import leji.bbslezy.vfx.forms.ExplosionForm;

import java.util.List;

/**
 * Adds the addon's form entries to BBS's "miscellaneous" (extra) form-picker section.
 */
@Mixin(ExtraFormSection.class)
public abstract class ExtraFormSectionMixin
{
    @Shadow public abstract List<FormCategory> getCategories();

    @Inject(method = "initiate", at = @At("TAIL"))
    private void bbslezy$addForms(CallbackInfo ci)
    {
        this.getCategories().get(0).addForm(new DestructionBoxForm());
        this.getCategories().get(0).addForm(new ExplosionForm());
    }
}
