package leji.bbslezy.server;

/**
 * Pure server-tick helpers for Right Ctrl playback.
 *
 * <p>Holds no Minecraft state so unit tests cover it.
 * Mixins delegate here and stay thin.</p>
 */
public class LezyServerTick
{
    /** Frame channels must match within this delta to count as identical. */
    public static final float EPSILON = 1.0E-4F;

    /** Ticks between {@code reviveLostActors()} scans. Actor loss is rare. */
    public static final int REVIVE_EVERY_N = 20;

    /** Movement below this squared length skips world collision. */
    public static final double MOVE_EPSILON_SQ = 1.0E-6D;

    public static boolean isIdentical(float[] a, float[] b)
    {
        if (a == null || b == null || a.length != b.length)
        {
            return false;
        }

        for (int i = 0; i < a.length; i++)
        {
            if (Math.abs(a[i] - b[i]) > EPSILON)
            {
                return false;
            }
        }

        return true;
    }

    public static boolean shouldRevive(int tickCounter)
    {
        return tickCounter % REVIVE_EVERY_N == 0;
    }

    public static boolean shouldCollide(double dx, double dy, double dz)
    {
        return dx * dx + dy * dy + dz * dz > MOVE_EPSILON_SQ;
    }
}
