package leji.bbslezy.vfx.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure physics math untuk blast impulse / DestructionBox.
 */
class DestructionPhysicsTest
{
    @Test
    void impulseFallsOffWithDistance()
    {
        float near = DestructionPhysics.blastImpulse(1F, 8F, 10F);
        float far  = DestructionPhysics.blastImpulse(6F, 8F, 10F);

        assertTrue(near > far, "Impulse should fall off with distance: near=" + near + ", far=" + far);
    }

    @Test
    void noBlastBeyondRadius()
    {
        assertEquals(0F, DestructionPhysics.blastImpulse(10F, 4F, 10F), 1e-6F);
    }
}
