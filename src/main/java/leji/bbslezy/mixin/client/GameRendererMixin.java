package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyNightVision;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    @Inject(method = "getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F", at = @At("HEAD"), cancellable = true)
    private static void bbslezy$getNightVisionStrength(LivingEntity entity, float delta, CallbackInfoReturnable<Float> cir)
    {
        if (LezyNightVision.isEnabled())
        {
            cir.setReturnValue(1.0F);
        }
    }
}
