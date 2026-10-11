package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.CracksVolume;
import leji.bbslezy.vfx.client.ExplosionSmokeVolume;
import leji.bbslezy.vfx.client.SmokeVolume;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Composite deferred VFX volumetric passes (Cracks, Smoke, and Explosion Mushroom/Fireball Volume)
 * at onWorldRenderEnd, mirroring BBSVFX's BBSRenderingImpactMixin.
 */
@Mixin(value = BBSRendering.class, remap = false)
public abstract class BBSRenderingVfxMixin
{
    @Inject(method = "onWorldRenderEnd", at = @At("HEAD"))
    private static void bbslezy$onWorldRenderEnd(CallbackInfo ci)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        CracksVolume.render(mc.getFramebuffer());
        SmokeVolume.render(mc.getFramebuffer());
        ExplosionSmokeVolume.render(mc.getFramebuffer());
    }
}
