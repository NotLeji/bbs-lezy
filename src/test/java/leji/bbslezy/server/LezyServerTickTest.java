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
}
