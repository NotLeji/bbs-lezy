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
    }

    @Test
    public void testQualityTiers()
    {
        // When setting is explicitly High (1)
        if (LodSettings.vfxQuality != null)
        {
            LodSettings.vfxQuality.set(1);
            assertEquals(LezyVfxQuality.TIER_HIGH, LezyVfxQuality.getEffectiveTier());
            assertEquals(56, LezyVfxQuality.getExplosionSteps());
            assertEquals(28, LezyVfxQuality.getWindSteps(true));
            assertEquals(16, LezyVfxQuality.getWindSteps(false));
            assertEquals(48, LezyVfxQuality.getBeamSteps());
            assertEquals(5, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(24, LezyVfxQuality.getImpactShapesMax());
            assertEquals(1.0F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(1.0F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(0, LezyVfxQuality.getPhysMaxBodiesCap(0));

            // Medium (2)
            LodSettings.vfxQuality.set(2);
            assertEquals(LezyVfxQuality.TIER_MEDIUM, LezyVfxQuality.getEffectiveTier());
            assertEquals(28, LezyVfxQuality.getExplosionSteps());
            assertEquals(20, LezyVfxQuality.getWindSteps(true));
            assertEquals(12, LezyVfxQuality.getWindSteps(false));
            assertEquals(24, LezyVfxQuality.getBeamSteps());
            assertEquals(3, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(16, LezyVfxQuality.getImpactShapesMax());
            assertEquals(0.5F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(0.65F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(5000, LezyVfxQuality.getPhysMaxBodiesCap(0));

            // Low (3)
            LodSettings.vfxQuality.set(3);
            assertEquals(LezyVfxQuality.TIER_LOW, LezyVfxQuality.getEffectiveTier());
            assertEquals(16, LezyVfxQuality.getExplosionSteps());
            assertEquals(14, LezyVfxQuality.getWindSteps(true));
            assertEquals(8, LezyVfxQuality.getWindSteps(false));
            assertEquals(16, LezyVfxQuality.getBeamSteps());
            assertEquals(2, LezyVfxQuality.getImpactBlurSamples());
            assertEquals(8, LezyVfxQuality.getImpactShapesMax());
            assertEquals(0.5F, LezyVfxQuality.getSilhouetteScale(), 0.001F);
            assertEquals(0.35F, LezyVfxQuality.getParticleThrottle(), 0.001F);
            assertEquals(2000, LezyVfxQuality.getPhysMaxBodiesCap(0));
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
            assertEquals(48, LezyVfxQuality.getBeamSteps());
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
