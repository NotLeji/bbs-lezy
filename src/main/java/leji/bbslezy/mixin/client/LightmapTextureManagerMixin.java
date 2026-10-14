package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyNightVision;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin
{
    @Redirect(method = "update(F)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z",
        ordinal = 0))
    private boolean bbslezy$nightVisionStatus(ClientPlayerEntity player, StatusEffect effect)
    {
        if (effect == StatusEffects.NIGHT_VISION && LezyNightVision.isEnabled())
        {
            return true;
        }

        return player != null && player.hasStatusEffect(effect);
    }
}
