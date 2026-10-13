package leji.bbslezy.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LezyServerTickTest
{
    @Test
    void zeroDisplacementSkipsCollision()
    {
        assertFalse(LezyServerTick.shouldCollide(0.0D, 0.0D, 0.0D));
        assertFalse(LezyServerTick.shouldCollide(1.0E-4D, 0.0D, 0.0D));
    }

    @Test
    void realDisplacementCollides()
    {
        assertTrue(LezyServerTick.shouldCollide(0.5D, 0.0D, 0.0D));
    }

    @Test
    void probeOnlyMoveSkipsCollision()
    {
        assertTrue(LezyServerTick.isProbeOnly(0.0D, -0.0784D, 0.0D));
    }

    @Test
    void realMoveStillCollides()
    {
        assertFalse(LezyServerTick.isProbeOnly(0.5D, -0.0784D, 0.0D));
        assertFalse(LezyServerTick.isProbeOnly(0.0D, -0.5D, 0.0D));
        assertFalse(LezyServerTick.isProbeOnly(0.0D, 0.0D, 0.0D));
    }

    @Test
    void bothEmptySkipsEquip()
    {
        assertTrue(LezyServerTick.skipEquipWhenBothEmpty(true, true));
    }

    @Test
    void nonEmptyNeedsEquip()
    {
        assertFalse(LezyServerTick.skipEquipWhenBothEmpty(false, true));
        assertFalse(LezyServerTick.skipEquipWhenBothEmpty(true, false));
        assertFalse(LezyServerTick.skipEquipWhenBothEmpty(false, false));
    }
}
