package leji.bbslezy.vfx.client;
import leji.bbslezy.vfx.forms.BeamForm;
import leji.bbslezy.vfx.forms.DestructionBoxForm;
import leji.bbslezy.vfx.forms.DomeForm;
import leji.bbslezy.vfx.forms.ExplosionForm;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.l10n.keys.IKey;

import java.util.Collections;
import java.util.List;

public class VfxFormSection extends FormSection
{
    private FormCategory vfxCategory;

    public VfxFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        this.vfxCategory = new FormCategory(IKey.constant("VFX"), this.parent.preferences.visible("vfx"))
            .icon(LezyVfxIcons.EXPLOSION);

        this.vfxCategory.addForm(new DestructionBoxForm());
        this.vfxCategory.addForm(new ExplosionForm());
        this.vfxCategory.addForm(new BeamForm());
        this.vfxCategory.addForm(new DomeForm());
    }

    @Override
    public List<FormCategory> getCategories()
    {
        return this.vfxCategory != null ? Collections.singletonList(this.vfxCategory) : Collections.emptyList();
    }
}
