package leji.bbslezy.perf;

/**
 * Pure decision core of the replay evaluation budget: no Minecraft or BBS types
 * flow through here, so the rules are pinned by plain unit tests. Ordering is
 * deliberate: protections first, then frustum, then distance buckets, then the
 * throttle phase.
 */
public final class PerfDecision
{
    public enum Verdict
    {
        FULL, THROTTLE, SKIP
    }

    public record Inputs(
        double distanceSq,
        boolean inFrustum,
        boolean shellActor,
        boolean anchoredByOther,
        boolean cameraLocked,
        boolean exception,
        long replayTicks,
        int throttleEvery,
        double throttleDistSq,
        double skipDistSq)
    {
        public Inputs withDistanceSq(double v)
        {
            return new Inputs(v, inFrustum, shellActor, anchoredByOther, cameraLocked, exception, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withFrustum(boolean v)
        {
            return new Inputs(distanceSq, v, shellActor, anchoredByOther, cameraLocked, exception, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withShellActor(boolean v)
        {
            return new Inputs(distanceSq, inFrustum, v, anchoredByOther, cameraLocked, exception, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withAnchoredByOther(boolean v)
        {
            return new Inputs(distanceSq, inFrustum, shellActor, v, cameraLocked, exception, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withCameraLocked(boolean v)
        {
            return new Inputs(distanceSq, inFrustum, shellActor, anchoredByOther, v, exception, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withException(boolean v)
        {
            return new Inputs(distanceSq, inFrustum, shellActor, anchoredByOther, cameraLocked, v, replayTicks, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withReplayTicks(long v)
        {
            return new Inputs(distanceSq, inFrustum, shellActor, anchoredByOther, cameraLocked, exception, v, throttleEvery, throttleDistSq, skipDistSq);
        }

        public Inputs withThrottleEvery(int v)
        {
            return new Inputs(distanceSq, inFrustum, shellActor, anchoredByOther, cameraLocked, exception, replayTicks, v, throttleDistSq, skipDistSq);
        }
    }

    public static Verdict evaluate(Inputs in)
    {
        if (in.exception() || in.shellActor() || in.anchoredByOther() || in.cameraLocked())
        {
            return Verdict.FULL;
        }

        if (!in.inFrustum())
        {
            return Verdict.SKIP;
        }

        if (in.throttleEvery() <= 0 || in.distanceSq() <= in.throttleDistSq())
        {
            return Verdict.FULL;
        }

        if (in.distanceSq() >= in.skipDistSq())
        {
            return Verdict.SKIP;
        }

        return in.replayTicks() % in.throttleEvery() == 0 ? Verdict.THROTTLE : Verdict.SKIP;
    }

    /**
     * Frame-path variant for startRenderFrame: only true SKIP (far / outside
     * frustum) leaves the list — THROTTLE renders like FULL so the per-frame
     * interpolation keeps the pose smooth.
     */
    public static Verdict evaluateFrame(Inputs in)
    {
        if (in.exception() || in.shellActor() || in.anchoredByOther() || in.cameraLocked())
        {
            return Verdict.FULL;
        }

        if (!in.inFrustum())
        {
            return Verdict.SKIP;
        }

        if (in.distanceSq() >= in.skipDistSq())
        {
            return Verdict.SKIP;
        }

        return Verdict.FULL;
    }

    /**
     * Shadow/nametag suppression only follows the distance/frustum SKIP bucket,
     * never the throttle bucket — so throttle_ticks=1 cannot permanently hide
     * mid-distance badges.
     */
    public static boolean isBadge(Inputs in)
    {
        if (in.exception() || in.shellActor() || in.anchoredByOther() || in.cameraLocked())
        {
            return false;
        }

        return !in.inFrustum() || in.distanceSq() >= in.skipDistSq();
    }

    private PerfDecision()
    {}
}
