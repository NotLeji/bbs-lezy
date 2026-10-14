package leji.bbslezy.mixin.client;

import leji.bbslezy.perf.ui.LezyUIBench;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame-time capture untuk LezyUIBench: no-op total tanpa bench flag.
 * 1.20.1: `MinecraftClient.render(boolean)` — deskriptor `render(Z)V`;
 * inject ke private method legal. Jangan pakai `render(ZZ)V` — tidak ada
 * di 1.20.1 dan inject bakal gagal diam-diam (F8).
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
    @Inject(method = "render(Z)V", at = @At("TAIL"))
    public void bbslezy$onRenderTail(CallbackInfo ci)
    {
        LezyUIBench.onFrameEnd();
    }
}
