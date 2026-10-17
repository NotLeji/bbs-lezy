package leji.bbslezy.vfx.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import leji.bbslezy.vfx.camera.BbsVfxImpactState;
import leji.bbslezy.vfx.client.BbsVfxFormBlend;
import leji.bbslezy.vfx.client.LezyImpactSilhouette;
import leji.bbslezy.vfx.client.SmearReplayState;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures every top-level actor form's render while an {@link leji.bbslezy.vfx.camera.ImpactClip} runs the
 * silhouette layer, so {@link LezyImpactSilhouette} can re-render them in isolation after the world pass.
 */
@Mixin(value = FormUtilsClient.class, remap = false)
public abstract class ImpactSilhouetteCaptureMixin
{
    @Inject(
        method = "render(Lmchorse/bbs_mod/forms/forms/Form;Lmchorse/bbs_mod/forms/renderers/FormRenderingContext;)V",
        at = @At("HEAD"))
    private static void bbslezy$captureSilhouette(Form form, FormRenderingContext context, CallbackInfo ci)
    {
        if (!BbsVfxImpactState.active || BbsVfxImpactState.silhouette <= 0.004F)
        {
            return;
        }

        if (LezyImpactSilhouette.replaying || BbsVfxFormBlend.replaying)
        {
            return;
        }

        if (BBSRendering.isIrisShadowPass())
        {
            return;
        }

        if (form == null || FormUtilsClient.getCurrentForm() != null)
        {
            return;
        }

        if (context.isPicking() || context.stencilMap != null)
        {
            return;
        }

        if (SmearReplayState.replay == null)
        {
            return;
        }

        if (BbsVfxImpactState.silTarget >= 0 && SmearReplayState.replayIndex != BbsVfxImpactState.silTarget)
        {
            return;
        }

        FormRenderer renderer = FormUtilsClient.getRenderer(form);

        if (renderer == null)
        {
            return;
        }

        LezyImpactSilhouette.defer(renderer, form, context.entity,
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()).mul(context.stack.peek().getPositionMatrix()),
            context.world != null ? new Matrix4f(context.world.peek().getPositionMatrix()) : null,
            context.light, context.color, context.transition);
    }
}
