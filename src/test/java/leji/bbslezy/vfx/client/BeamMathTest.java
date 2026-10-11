package leji.bbslezy.vfx.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure math closed-form untuk Beam form (Port BBSVFX D2). */
class BeamMathTest
{
    @Test
    void ringRiseIsDeterminedByProgress()
    {
        /* ring rise harus murni fungsi dari progress + index — test hanya deterministik. */
        float a = LezyBeamMath.ringOffset(0.3F, 0);
        float b = LezyBeamMath.ringOffset(0.3F, 0);

        assertEquals(a, b, 1e-6F, "ring offset harus closed-form (deterministik)");
    }

    @Test
    void ringOffsetsMonotonicallyRank()
    {
        assertTrue(LezyBeamMath.ringOffset(0.5F, 0) < LezyBeamMath.ringOffset(0.5F, 1));
    }
}
