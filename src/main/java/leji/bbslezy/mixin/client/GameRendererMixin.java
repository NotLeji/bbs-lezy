package leji.bbslezy.mixin.client;

import leji.bbslezy.LodSettings;
import leji.bbslezy.perf.ui.LezyUIWorldSkip;
import leji.bbslezy.ui.LezyNightVision;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    /**
     * Skip render world di balik dashboard opak (F0b, Plan/dashboard-ui-perf.md
     * Task 6): kondisi eksklusif dari LezyUIWorldSkip.shouldSkip — saat
     * dashboard panel menutup window penuh dengan background solid, hasil
     * render world tidak terlihat, jadi dipangkas; kondisi apa pun yang
     * menampakkan world (tint transluen, background image, editor film) tetap
     * dirender. Fail-open: race render normal kalau apapun selain skip terjadi.
     */
    @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipWorldBehindOpaqueDashboard(net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci)
    {
        try
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (!(mc.currentScreen instanceof UIScreen) || !bbslezy$worldSkipOn())
            {
                return;
            }

            if (!(UIScreen.getCurrentMenu() instanceof UIDashboard dashboard))
            {
                return;
            }

            boolean panelNeedsBackground = dashboard.getPanels().panel != null
                && dashboard.getPanels().panel.needsBackground();

            if (BBSSettings.backgroundImage.get() != null)
            {
                return; /* background image: world masih bisa terlihat di baliknya */
            }

            float tintAlpha = Colors.getA(BBSSettings.backgroundColor.get());

            if (LezyUIWorldSkip.shouldSkip(true, true, panelNeedsBackground, tintAlpha))
            {
                ci.cancel();
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(); /* fail-open: world tetap dirender */
        }
    }

    /**
     * Guard setting world-skip: null atau nilai true berarti ON.
     * @Unique mencegah konflik merge jadi target vanilla.
     */
    @Unique
    private static boolean bbslezy$worldSkipOn()
    {
        return LodSettings.ui_world_skip == null
            || LodSettings.ui_world_skip.get();
    }
}
