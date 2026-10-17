package leji.bbslezy.perf;

import org.junit.jupiter.api.Test;

import static leji.bbslezy.perf.PerfDecision.Verdict.FULL;
import static leji.bbslezy.perf.PerfDecision.Verdict.SKIP;
import static leji.bbslezy.perf.PerfDecision.Verdict.THROTTLE;
import static leji.bbslezy.perf.PerfDecision.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfDecisionTest
{
    private static PerfDecision.Inputs base()
    {
        /* distanceSq 4000.0 (~63 blocks) — inside throttle band between 32² (1024) and 96² (9216). */
        return new PerfDecision.Inputs(4000.0, true, false, false, false, false, 20L, 4, 1024.0, 9216.0);
    }

    @Test
    public void closeInFrustumIsFull()
    {
        assertEquals(FULL, evaluate(base().withDistanceSq(100.0)));
    }

    @Test
    public void middleDistancePhaseHitIsThrottle()
    {
        assertEquals(THROTTLE, evaluate(base()));
    }

    @Test
    public void middleDistancePhaseMissIsSkip()
    {
        assertEquals(SKIP, evaluate(base().withReplayTicks(21L)));
    }

    @Test
    public void farDistanceIsSkipRegardlessOfPhase()
    {
        assertEquals(SKIP, evaluate(base().withDistanceSq(2000000.0).withReplayTicks(20L)));
    }

    @Test
    public void outsideFrustumIsSkipEvenWhenClose()
    {
        assertEquals(SKIP, evaluate(base().withDistanceSq(100.0).withFrustum(false)));
    }

    @Test
    public void shellActorAlwaysFull()
    {
        assertEquals(FULL, evaluate(base().withDistanceSq(2000000.0).withShellActor(true)));
    }

    @Test
    public void anchorTargetAlwaysFull()
    {
        assertEquals(FULL, evaluate(base().withDistanceSq(2000000.0).withAnchoredByOther(true)));
    }

    @Test
    public void cameraLockedAlwaysFull()
    {
        assertEquals(FULL, evaluate(base().withDistanceSq(2000000.0).withCameraLocked(true)));
    }

    @Test
    public void exceptionAlwaysFull()
    {
        assertEquals(FULL, evaluate(base().withDistanceSq(2000000.0).withException(true)));
    }

    @Test
    public void disabledPolicyIsFull()
    {
        assertEquals(FULL, evaluate(base().withThrottleEvery(0)));
    }

    @Test
    public void skipDistanceGateBeatsThrottleWindow()
    {
        assertEquals(SKIP, evaluate(base().withDistanceSq(9217.0).withReplayTicks(20L)));
    }

    @Test
    public void throttlePhaseMissStillFullInFrameMode()
    {
        assertEquals(FULL, PerfDecision.evaluateFrame(base().withReplayTicks(21L)));
    }

    @Test
    public void skipStaysSkipInFrameMode()
    {
        assertEquals(SKIP, PerfDecision.evaluateFrame(base().withDistanceSq(2000000.0)));
    }

    @Test
    public void badgeOnlyForSkipVerdictNotThrottle()
    {
        assertFalse(PerfDecision.isBadge(base()));
        assertFalse(PerfDecision.isBadge(base().withThrottleEvery(1)));
        assertTrue(PerfDecision.isBadge(base().withDistanceSq(2000000.0)));
    }
}
