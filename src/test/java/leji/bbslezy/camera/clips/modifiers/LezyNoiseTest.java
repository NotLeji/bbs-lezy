package leji.bbslezy.camera.clips.modifiers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LezyNoiseTest
{
    @Test
    void noise_deterministic()
    {
        double v1 = LezyNoise.noise(12.345D, 42);
        double v2 = LezyNoise.noise(12.345D, 42);
        assertEquals(v1, v2, 1e-9D, "Noise must be deterministic for identical inputs");
    }

    @Test
    void noise_range()
    {
        for (double x = -10D; x <= 10D; x += 0.25D)
        {
            double v = LezyNoise.noise(x, 123);
            assertTrue(v >= -1.0D && v <= 1.0D, "Noise output must be in [-1, 1], got: " + v);
        }
    }

    @Test
    void noise_seedDiffers()
    {
        double v1 = LezyNoise.noise(5.5D, 1);
        double v2 = LezyNoise.noise(5.5D, 2);
        assertNotEquals(v1, v2, "Different seeds should produce different values");
    }

    @Test
    void fbm_rangeAndDeterminism()
    {
        for (double x = 0D; x <= 20D; x += 0.5D)
        {
            double f1 = LezyNoise.fbm(x, 77, 3, 0.5D);
            double f2 = LezyNoise.fbm(x, 77, 3, 0.5D);
            assertEquals(f1, f2, 1e-9D, "FBM must be deterministic");
            assertTrue(f1 >= -1.0D && f1 <= 1.0D, "FBM normalized output must be in [-1, 1], got: " + f1);
        }
    }

    @Test
    void fbm_octavesImpact()
    {
        double fbm1 = LezyNoise.fbm(3.7D, 10, 1, 0.5D);
        double fbm3 = LezyNoise.fbm(3.7D, 10, 4, 0.5D);
        assertNotEquals(fbm1, fbm3, "Different octaves should produce different details");
    }
}
