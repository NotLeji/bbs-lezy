package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.forms.forms.Form;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import leji.bbslezy.vfx.forms.LezyWindForm;

/**
 * Exempts ModelBlockEntity with LezyWindForm from bounding box culling so zonal wind placed as a block
 * remains active and rendered even when looking away from the model block itself.
 */
@Mixin(value = ModelBlockEntityRenderer.class, remap = false)
public abstract class ModelBlockEntityRendererVfxMixin
{
    @Inject(method = "rendersOutsideBoundingBox", at = @At("HEAD"), cancellable = true)
    private void bbslezy$rendersWindOutsideBoundingBox(ModelBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir)
    {
        if (blockEntity != null && blockEntity.getProperties() != null)
        {
            Form form = blockEntity.getProperties().getForm();

            if (form instanceof LezyWindForm)
            {
                cir.setReturnValue(true);
            }
        }
    }
    @Inject(method = "canRender", at = @At("HEAD"), cancellable = true)
    private void bbslezy$alwaysRenderWindModelBlock(ModelBlockEntity entity, CallbackInfoReturnable<Boolean> cir)
    {
        if (entity != null && entity.getProperties() != null)
        {
            Form form = entity.getProperties().getForm();

            if (form instanceof LezyWindForm && entity.getProperties().isEnabled())
            {
                cir.setReturnValue(true);
            }
        }
    }
}
