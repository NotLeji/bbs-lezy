package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyIrisHelper;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BBSRendering.class, remap = false)
public abstract class BBSRenderingMixin
{
    @Inject(method = "toggleFramebuffer(Z)V", at = @At("TAIL"))
    private static void bbslezy$onToggleFramebuffer(boolean toggleFramebuffer, CallbackInfo ci)
    {
        LezyIrisHelper.syncPipelineDepthTarget();
    }

    @Inject(method = "onWorldRenderBegin()V", at = @At("TAIL"))
    private static void bbslezy$onWorldRenderBegin(CallbackInfo ci)
    {
        if (BBSModClient.getCameraController().getCurrent() != null || BBSRendering.isCustomSize())
        {
            MinecraftClient.getInstance().gameRenderer.setRenderHand(false);
        }
    }

    @Inject(method = "onWorldRenderEnd()V", at = @At("TAIL"))
    private static void bbslezy$onWorldRenderEnd(CallbackInfo ci)
    {
        MinecraftClient.getInstance().gameRenderer.setRenderHand(true);
    }
}
