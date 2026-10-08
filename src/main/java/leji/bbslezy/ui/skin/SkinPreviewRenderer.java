package leji.bbslezy.ui.skin;

import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;

public class SkinPreviewRenderer extends UIFormRenderer
{
    public SkinPreviewRenderer(Link textureLink, boolean isAlex)
    {
        ModelForm modelForm = new ModelForm();
        modelForm.model.set(isAlex ? "player/alex" : "player/steve");
        modelForm.texture.set(textureLink);
        this.form = modelForm;
        this.grid = false;
        this.setDistance(2.25F);
        this.setPosition(0.0F, 1.0F, 0.0F);
        this.setRotation(0.0F, 0.0F);
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        super.renderUserModel(context);
    }
}
