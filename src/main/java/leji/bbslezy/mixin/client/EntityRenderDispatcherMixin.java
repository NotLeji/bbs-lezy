package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyPreviewSnap;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void bbslezy$shouldRenderSnapPlayer(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir)
    {
        if (LezyPreviewSnap.shouldHidePlayer(entity))
        {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void bbslezy$cancelRenderSnapPlayer(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci)
    {
        if (LezyPreviewSnap.shouldHidePlayer(entity))
        {
            ci.cancel();
        }
    }
}
