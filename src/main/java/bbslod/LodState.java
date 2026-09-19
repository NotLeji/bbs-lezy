package bbslod;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-render tier stack, read by the Phase 4 mixin.
 *
 * <p>Render-thread only, and a stack rather than a plain field on purpose: {@code FormUtilsClient.render}
 * is reentrant through {@code renderBodyParts}, and {@code ModelFormRenderer} renders body parts inside
 * its own pass, so a single static field would be clobbered by the nesting.</p>
 */
public class LodState
{
    private static final Deque<Integer> tiers = new ArrayDeque<>();

    /** Bumped by the Phase 4 mixin every bone it skips, so silent mixin degradation is visible. */
    public static long mixinHits;

    public static void push(int tier)
    {
        tiers.push(tier);
    }

    public static void pop()
    {
        if (!tiers.isEmpty())
        {
            tiers.pop();
        }
    }

    public static int current()
    {
        return tiers.isEmpty() ? 0 : tiers.peek();
    }
}
