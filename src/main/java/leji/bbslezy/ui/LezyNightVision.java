package leji.bbslezy.ui;

import leji.bbslezy.LodSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;

/**
 * Controller for client-side Night Vision (Fullbright) toggle.
 * Provides maximum world brightness without status effects, potion swirl particles,
 * or HUD status icons.
 */
public final class LezyNightVision
{
    private static final StatusEffectInstance FAKE_NIGHT_VISION = new StatusEffectInstance(
        StatusEffects.NIGHT_VISION,
        32767,
        0,
        false,
        false,
        false
    );

    private static boolean enabled = false;

    public static boolean isEnabled()
    {
        if (LodSettings.nightVision != null && LodSettings.nightVision.get() != enabled)
        {
            enabled = LodSettings.nightVision.get();
        }

        return enabled;
    }

    public static StatusEffectInstance getFakeInstance()
    {
        return FAKE_NIGHT_VISION;
    }

    public static void setEnabled(boolean value)
    {
        enabled = value;

        if (LodSettings.nightVision != null && LodSettings.nightVision.get() != value)
        {
            LodSettings.nightVision.set(value);
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getLightmapTextureManager() != null)
        {
            mc.gameRenderer.getLightmapTextureManager().tick();
        }
    }

    public static void toggle()
    {
        setEnabled(!isEnabled());

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.player != null)
        {
            String status = enabled ? "§e[BBS Lezy] Night Vision: ON" : "§7[BBS Lezy] Night Vision: OFF";
            mc.player.sendMessage(Text.literal(status), true);
        }
    }

    private LezyNightVision()
    {}
}
