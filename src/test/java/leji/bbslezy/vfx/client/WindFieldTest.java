package leji.bbslezy.vfx.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindFieldTest
{
    @Test
    void tornadoSwirlTangentialVelocity_nonzero()
    {
        float v = LezyWindMath.tangential(1F, 0F, 1F);
        assertTrue(v != 0F);
        assertEquals(1.0F, v, 1e-4F);
    }

    @Test
    void tornadoSwirlTangentialVelocity_falloffOutsideCore()
    {
        float core = 2.0F;
        float inside = LezyWindMath.tangential(1.0F, core);
        float atCore = LezyWindMath.tangential(2.0F, core);
        float outside = LezyWindMath.tangential(4.0F, core);

        assertEquals(0.5F, inside, 1e-4F);
        assertEquals(1.0F, atCore, 1e-4F);
        assertEquals(0.5F, outside, 1e-4F);
    }
}
