package leji.bbslezy.server;

/**
 * Pure server-tick helpers for Right Ctrl playback.
 *
 * <p>Holds no Minecraft state so unit tests cover it.
 * Mixins delegate here and stay thin.</p>
 */
public class LezyServerTick
{
    /** BBS gravity probe, mirrors ReplayKeyframes.GRAVITY_PROBE without the dep. */
    public static final double GRAVITY_PROBE = 0.0784D;

    /** Movement below this squared length skips world collision. */
    public static final double MOVE_EPSILON_SQ = 1.0E-6D;

    public static boolean shouldCollide(double dx, double dy, double dz)
    {
        return dx * dx + dy * dy + dz * dz > MOVE_EPSILON_SQ && !isProbeOnly(dx, dy, dz);
    }

    public static boolean isProbeOnly(double dx, double dy, double dz)
    {
        return Math.abs(dx) < 0.001D && Math.abs(dz) < 0.001D && Math.abs(dy + GRAVITY_PROBE) < 0.001D;
    }

    public static boolean skipEquipWhenBothEmpty(boolean currentEmpty, boolean incomingEmpty)
    {
        return currentEmpty && incomingEmpty;
    }
}
