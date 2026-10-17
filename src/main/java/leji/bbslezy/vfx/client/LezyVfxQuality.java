package leji.bbslezy.vfx.client;

import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.MinecraftClient;

/**
 * Global VFX quality resolver. Determines whether preview effects should render at
 * maximum fidelity or at optimized tiers (High / Medium / Low) based on user settings
 * and real-time FPS detection.
 *
 * <p>★Export and recording ALWAYS run at maximum fidelity (High) regardless of settings.</p>
 */
public final class LezyVfxQuality
{
    public static final int TIER_HIGH = 0;
    public static final int TIER_MEDIUM = 1;
    public static final int TIER_LOW = 2;

    private static int dynamicTier = TIER_HIGH;
    private static long lastFpsCheck = 0L;
    private static int lowFpsTicks = 0;
    private static int goodFpsTicks = 0;

    private LezyVfxQuality()
    {}

    /**
     * True when video recording or video export is currently active, or when render limit is 0 (export mode).
     */
    public static boolean isExporting()
    {
        try
        {
            if (BBSModClient.getVideoRecorder() != null && BBSModClient.getVideoRecorder().isRecording())
            {
                return true;
            }
        }
        catch (Throwable ignored)
        {}

        if (LodSettings.renderLimit != null && LodSettings.renderLimit.get() == 0)
        {
            return true;
        }

        return false;
    }

    /**
     * Resolves the current effective quality tier (0 = High, 1 = Medium, 2 = Low).
     */
    public static void setDynamicTierForTest(int tier)
    {
        dynamicTier = Math.max(TIER_HIGH, Math.min(TIER_LOW, tier));
    }

    public static int getDynamicTier()
    {
        return dynamicTier;
    }

    public static int getEffectiveTier()
    {
        if (isExporting())
        {
            return TIER_HIGH;
        }

        int setting = LodSettings.vfxQuality != null ? LodSettings.vfxQuality.get() : 0;
        int baseTier = switch (setting)
        {
            case 1 -> TIER_HIGH;
            case 2 -> TIER_MEDIUM;
            case 3 -> TIER_LOW;
            default -> TIER_HIGH;
        };

        boolean dynamicEnabled = LodSettings.vfxDynamicQuality == null || LodSettings.vfxDynamicQuality.get();

        if (!dynamicEnabled)
        {
            return baseTier;
        }

        return Math.max(baseTier, dynamicTier);
    }

    /**
     * Evaluates real-time client FPS and dynamically shifts quality tier when auto is enabled.
     */
    public static void tick(MinecraftClient client)
    {
        if (client == null)
        {
            return;
        }

        boolean dynamicEnabled = LodSettings.vfxDynamicQuality == null || LodSettings.vfxDynamicQuality.get();

        if (!dynamicEnabled || isExporting())
        {
            dynamicTier = TIER_HIGH;
            lowFpsTicks = 0;
            goodFpsTicks = 0;
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastFpsCheck < 500L)
        {
            return;
        }

        lastFpsCheck = now;
        int fps = client.getCurrentFps();

        if (fps < 32 && fps > 0)
        {
            lowFpsTicks++;
            goodFpsTicks = 0;

            if (lowFpsTicks >= 2 && dynamicTier < TIER_LOW)
            {
                dynamicTier++;
                lowFpsTicks = 0;
            }
        }
        else if (fps > 48)
        {
            goodFpsTicks++;
            lowFpsTicks = 0;

            if (goodFpsTicks >= 4 && dynamicTier > TIER_HIGH)
            {
                dynamicTier--;
                goodFpsTicks = 0;
            }
        }
        else
        {
            lowFpsTicks = 0;
            goodFpsTicks = 0;
        }
    }

    public static int getExplosionSteps()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 16;
            case TIER_MEDIUM -> 28;
            default -> 56;
        };
    }

    public static int getWindSteps(boolean vortex)
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> vortex ? 14 : 8;
            case TIER_MEDIUM -> vortex ? 20 : 12;
            default -> vortex ? 28 : 16;
        };
    }

    public static int getBeamSteps()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 16;
            case TIER_MEDIUM -> 24;
            default -> 48;
        };
    }

    public static int getSmokeSteps()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 8;
            case TIER_MEDIUM -> 14;
            default -> 20;
        };
    }

    public static int getCracksSteps()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 10;
            case TIER_MEDIUM -> 16;
            default -> 24;
        };
    }

    public static int getImpactBlurSamples()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 2;
            case TIER_MEDIUM -> 3;
            default -> 5;
        };
    }

    public static int getImpactShapesMax()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 8;
            case TIER_MEDIUM -> 16;
            default -> 24;
        };
    }

    public static float getSilhouetteScale()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 0.5F;
            case TIER_MEDIUM -> 0.5F;
            default -> 1.0F;
        };
    }

    public static float getParticleThrottle()
    {
        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 0.35F;
            case TIER_MEDIUM -> 0.65F;
            default -> 1.0F;
        };
    }

    public static int getPhysMaxBodiesCap(int configured)
    {
        if (configured > 0)
        {
            return configured;
        }

        if (isExporting())
        {
            return 0;
        }

        return switch (getEffectiveTier())
        {
            case TIER_LOW -> 2000;
            case TIER_MEDIUM -> 5000;
            default -> 0;
        };
    }
}
