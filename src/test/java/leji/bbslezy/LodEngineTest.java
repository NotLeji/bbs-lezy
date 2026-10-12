package leji.bbslezy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodEngineTest
{
    @Test
    void testBehindCameraPenalized()
    {
        double front = LodEngine.score(10D, 1D, 0D);
        double behind = LodEngine.score(10D, -1D, 0D);

        assertEquals(10D, front, 0.001);
        assertTrue(behind > front + 1000D);
    }

    @Test
    void testFocusDistance()
    {
        assertEquals(0D, LodEngine.score(50D, 1D, 50D), 0.001);
        assertEquals(10D, LodEngine.score(60D, 1D, 50D), 0.001);
    }
}
