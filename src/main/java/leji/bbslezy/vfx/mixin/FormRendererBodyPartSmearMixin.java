package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.SmearRenderState;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Per-bone smear attachments isolation.
 */
@Mixin(value = FormRenderer.class, remap = false)
public abstract class FormRendererBodyPartSmearMixin
{
    @Inject(method = "renderBodyPart", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipHiddenBoneParts(BodyPart part, FormRenderingContext context, CallbackInfo ci)
    {
        if (!SmearRenderState.active || (Object) this != SmearRenderState.target)
        {
            return;
        }

        if (part.getForm() instanceof TrailForm)
        {
            ci.cancel();
            return;
        }

        if (SmearRenderState.hide == null)
        {
            return;
        }

        String bone = part.bone.get();

        if (bone == null || bone.isEmpty() || SmearRenderState.hide.contains(bone))
        {
            ci.cancel();
        }
    }
}
