package leji.bbslezy.vfx.forms;

import mchorse.bbs_mod.data.types.MapType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SmearProperties pose round-trip and factory checks (Port BBSVFX D5).
 */
class SmearPropertiesTest
{
    @Test
    void poseRoundTripPreservesCountAndStrength()
    {
        SmearProperties p = new SmearProperties("test_smear");
        p.enabled.set(true);
        p.count.set(8);
        p.falloff.set(0.75F);
        p.amount.set(2.5F);
        p.manual.set(true);
        p.manualX.set(1.2F);
        p.manualY.set(3.4F);
        p.manualZ.set(5.6F);

        MapType data = (MapType) p.toData();

        SmearProperties restored = new SmearProperties("test_smear");
        restored.fromData(data);

        assertTrue(restored.enabled.get());
        assertEquals(8, restored.count.get());
        assertEquals(0.75F, restored.falloff.get(), 1e-5F);
        assertEquals(2.5F, restored.amount.get(), 1e-5F);
        assertTrue(restored.manual.get());
        assertEquals(1.2F, restored.manualX.get(), 1e-5F);
        assertEquals(3.4F, restored.manualY.get(), 1e-5F);
        assertEquals(5.6F, restored.manualZ.get(), 1e-5F);
    }

    @Test
    void poseFactoriesAreDistinct()
    {
        assertNotNull(LezyVfxPoseFactories.SMEAR);
        assertNotNull(LezyVfxPoseFactories.LINES);
        assertNotNull(LezyVfxPoseFactories.FORM_SMEAR);
        assertNotSame(LezyVfxPoseFactories.SMEAR, LezyVfxPoseFactories.LINES);
        assertNotSame(LezyVfxPoseFactories.SMEAR, LezyVfxPoseFactories.FORM_SMEAR);
        assertNotSame(LezyVfxPoseFactories.LINES, LezyVfxPoseFactories.FORM_SMEAR);
    }
}
