package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyNightVision;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    public void bbslezy$hasStatusEffect(StatusEffect effect, CallbackInfoReturnable<Boolean> cir)
    {
        if (effect == StatusEffects.NIGHT_VISION && LezyNightVision.isEnabled())
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && ((Object) this == mc.player || (Object) this == mc.cameraEntity))
            {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
    public void bbslezy$getStatusEffect(StatusEffect effect, CallbackInfoReturnable<StatusEffectInstance> cir)
    {
        if (effect == StatusEffects.NIGHT_VISION && LezyNightVision.isEnabled())
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && ((Object) this == mc.player || (Object) this == mc.cameraEntity))
            {
                cir.setReturnValue(LezyNightVision.getFakeInstance());
            }
        }
    }
}
