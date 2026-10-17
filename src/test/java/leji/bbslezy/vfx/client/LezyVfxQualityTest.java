package leji.bbslezy.vfx.client;

import leji.bbslezy.LodSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LezyVfxQualityTest
{
    @BeforeEach
    public void setup()
    {
        if (LodSettings.renderLimit != null)
        {
            LodSettings.renderLimit.set(100);
        }
        if (LodSettings.vfxQuality != null)
        {
            LodSettings.vfxQuality.set(0); // Auto
        }
        if (LodSettings.vfxDynamicQuality != null)
        {
            LodSettings.vfxDynamicQuality.set(false); // disable dynamic shift by default in tests
        }
        LezyVfxQuality.setDynamicTierForTest(LezyVfxQuality.TIER_HIGH);
    }

    @Test
    public void testQualityTiers()
    {
        if (LodSettings.vfxQuality != null)
        {
            // High (1)
            LodSettings.vfxQuality.set(1);
            assertEquals(LezyVfxQuality.TIER_HIGH, LezyVfxQuality.getEffectiveTier());
            assertEquals(36, LezyVfxQuality.getExplosionSteps());
            assertEquals(4, LezyVfxQuality.getExplosionOctaves());
            assertEquals(28, LezyVfxQuality.getWindSteps(true));
            assertEquals(16, LezyVfxQuality.getWindSteps(false));
            assertEquals(48, LezyVfxQuality.getBeamSteps());
            assertEquals(20, LezyVfxQuality.getSmokeSteps());
            assertEquals(24, LezyVfxQuality.getCracksSteps());
            assertEquals(5, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(24, LezyVfxQuality.getImpactShapesMax());
            assertEquals(1.0F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(1.0F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(0, LezyVfxQuality.getPhysMaxBodiesCap(0));

            // Medium (2)
            LodSettings.vfxQuality.set(2);
            assertEquals(LezyVfxQuality.TIER_MEDIUM, LezyVfxQuality.getEffectiveTier());
            assertEquals(18, LezyVfxQuality.getExplosionSteps());
            assertEquals(3, LezyVfxQuality.getExplosionOctaves());
            assertEquals(20, LezyVfxQuality.getWindSteps(true));
            assertEquals(12, LezyVfxQuality.getWindSteps(false));
            assertEquals(24, LezyVfxQuality.getBeamSteps());
            assertEquals(14, LezyVfxQuality.getSmokeSteps());
            assertEquals(16, LezyVfxQuality.getCracksSteps());
            assertEquals(3, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(16, LezyVfxQuality.getImpactShapesMax());
            assertEquals(0.5F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(0.65F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(5000, LezyVfxQuality.getPhysMaxBodiesCap(0));

            // Low (3)
            LodSettings.vfxQuality.set(3);
            assertEquals(LezyVfxQuality.TIER_LOW, LezyVfxQuality.getEffectiveTier());
            assertEquals(10, LezyVfxQuality.getExplosionSteps());
            assertEquals(2, LezyVfxQuality.getExplosionOctaves());
            assertEquals(14, LezyVfxQuality.getWindSteps(true));
            assertEquals(8, LezyVfxQuality.getWindSteps(false));
            assertEquals(16, LezyVfxQuality.getBeamSteps());
            assertEquals(8, LezyVfxQuality.getSmokeSteps());
            assertEquals(10, LezyVfxQuality.getCracksSteps());
            assertEquals(2, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(8, LezyVfxQuality.getImpactShapesMax());
            assertEquals(0.5F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(0.35F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(2000, LezyVfxQuality.getPhysMaxBodiesCap(0));
        }
    }

    @Test
    public void testDynamicQualityToggle()
    {
        if (LodSettings.vfxQuality != null && LodSettings.vfxDynamicQuality != null)
        {
            LodSettings.vfxQuality.set(1); // High base
            LezyVfxQuality.setDynamicTierForTest(LezyVfxQuality.TIER_LOW);

            // With dynamic quality OFF, effective tier stays at baseTier (High)
            LodSettings.vfxDynamicQuality.set(false);
            assertEquals(LezyVfxQuality.TIER_HIGH, LezyVfxQuality.getEffectiveTier());

            // With dynamic quality ON, drops to degraded dynamicTier (Low)
            LodSettings.vfxDynamicQuality.set(true);
            assertEquals(LezyVfxQuality.TIER_LOW, LezyVfxQuality.getEffectiveTier());

            // Under Auto (0), follows dynamicTier when ON
            LodSettings.vfxQuality.set(0);
            assertEquals(LezyVfxQuality.TIER_LOW, LezyVfxQuality.getEffectiveTier());

            // Under Auto (0), defaults to High when toggle is OFF
            LodSettings.vfxDynamicQuality.set(false);
            assertEquals(LezyVfxQuality.TIER_HIGH, LezyVfxQuality.getEffectiveTier());
        }
    }

    @Test
    public void testExportOverridesQuality()
    {
        if (LodSettings.renderLimit != null && LodSettings.vfxQuality != null)
        {
            // Set quality to Low
            LodSettings.vfxQuality.set(3);
            assertEquals(LezyVfxQuality.TIER_LOW, LezyVfxQuality.getEffectiveTier());

            // During export, renderLimit is 0
            LodSettings.renderLimit.set(0);
            assertTrue(LezyVfxQuality.isExporting());
            assertEquals(LezyVfxQuality.TIER_HIGH, LezyVfxQuality.getEffectiveTier());
            assertEquals(56, LezyVfxQuality.getExplosionSteps());
            assertEquals(4, LezyVfxQuality.getExplosionOctaves());
            assertEquals(48, LezyVfxQuality.getBeamSteps());
            assertEquals(20, LezyVfxQuality.getSmokeSteps());
            assertEquals(24, LezyVfxQuality.getCracksSteps());
            assertEquals(5, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(24, LezyVfxQuality.getImpactShapesMax());
            assertEquals(1.0F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(0, LezyVfxQuality.getPhysMaxBodiesCap(0)); // unlimited
        }
    }

    @Test
    public void testConfiguredBodiesPreserved()
    {
        // When user explicitly sets physMaxBodies > 0, it must not be clamped down
        assertEquals(1234, LezyVfxQuality.getPhysMaxBodiesCap(1234));
    }
}
