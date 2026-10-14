package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyNightVision;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin
{
    @Redirect(method = "render(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    private static boolean bbslezy$fogNightVision(LivingEntity entity, StatusEffect effect)
    {
        if (effect == StatusEffects.NIGHT_VISION && LezyNightVision.isEnabled())
        {
            return true;
        }

        return entity != null && entity.hasStatusEffect(effect);
    }
}
