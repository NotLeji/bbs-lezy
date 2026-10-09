package leji.bbslezy.camera.clips.modifiers;

/**
 * Procedural 1D value noise and fractal Brownian motion (fbm).
 * Self-written with independent hash constants, smoothstep interpolation,
 * and normalized amplitude scaling.
 */
public class LezyNoise
{
    public static double noise(double x, int seed)
    {
        int cell = (int) Math.floor(x);
        double t = x - cell;
        t = t * t * (3D - 2D * t); /* smoothstep */
        return lerp(hash(cell, seed), hash(cell + 1, seed), t);
    }

    public static double fbm(double x, int seed, int octaves, double gain)
    {
        double sum = 0D;
        double amp = 1D;
        double max = 0D;

        for (int i = 0; i < octaves; i++)
        {
            sum += amp * noise(x * Math.pow(2D, i), seed + i * 101);
            max += amp;
            amp *= gain;
        }

        return max > 0D ? sum / max : 0D; /* normalized to [-1, 1] */
    }

    private static double hash(int cell, int seed)
    {
        long h = (cell * 0x9E3779B1L) ^ (seed * 0x85EBCA6BL);
        h = (h ^ (h >>> 13)) * 0x27D4EB2FL;
        h ^= h >>> 16;
        return ((h & 0xFFFFFFFFL) / (double) (1L << 32)) * 2D - 1D;
    }

    private static double lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }
}
