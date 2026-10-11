package leji.bbslezy.vfx.client;

public final class LezyWindMath
{
    private LezyWindMath() {}

    /**
     * Rankine tangential profile: linear r/core inside core, core/r outside core.
     */
    public static float tangential(float r, float core)
    {
        float c = Math.max(core, 0.5F);
        return r < c ? (r / c) : (c / r);
    }

    public static float tangential(float lx, float lz, float core)
    {
        float r = (float) Math.sqrt(lx * lx + lz * lz);
        return tangential(r, core);
    }
}
