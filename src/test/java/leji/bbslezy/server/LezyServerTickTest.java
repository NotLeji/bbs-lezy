package leji.bbslezy.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LezyServerTickTest
{
    @Test
    void identicalFrameSkipsApply()
    {
        float[] a = {10.0F, 64.0F, 10.0F, 90.0F, 0.0F};
        float[] b = {10.0F, 64.0F, 10.0F, 90.0F, 0.0F};

        assertTrue(LezyServerTick.isIdentical(a, b));
    }

    @Test
    void movedFrameNeedsApply()
    {
        float[] a = {10.0F, 64.0F, 10.0F, 90.0F, 0.0F};
        float[] b = {11.0F, 64.0F, 10.0F, 90.0F, 0.0F};

        assertFalse(LezyServerTick.isIdentical(a, b));
    }

    @Test
    void nullFrameNeverSkips()
    {
        assertFalse(LezyServerTick.isIdentical(null, new float[5]));
        assertFalse(LezyServerTick.isIdentical(new float[5], null));
    }

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
    void reviveThrottledToOncePerSecond()
    {
        assertTrue(LezyServerTick.shouldRevive(20));
        assertTrue(LezyServerTick.shouldRevive(40));
        assertFalse(LezyServerTick.shouldRevive(21));
        assertFalse(LezyServerTick.shouldRevive(1));
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
