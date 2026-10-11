package leji.bbslezy.vfx.client;

/**
 * Pure closed-form math helpers extracted from BeamFormRenderer for determinism & testing.
 */
public final class LezyBeamMath
{
    private LezyBeamMath() {}

    /**
     * Compute ring rise offset for a given progress / time and ring index.
     * Deterministic, monotonically increasing with index within base phase.
     */
    public static float ringOffset(float progress, int index)
    {
        float ph = progress + index * 0.25F;
        return ph;
    }

    /**
     * Cyclic phase fraction in [0, 1).
     */
    public static float cyclicFraction(float progress, int index, int count)
    {
        if (count <= 0) return 0F;
        float ph = progress + (float) index / (float) count;
        return ph - (float) Math.floor(ph);
    }
}
