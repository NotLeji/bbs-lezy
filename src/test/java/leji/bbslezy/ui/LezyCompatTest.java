package leji.bbslezy.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LezyCompatTest
{
    @Test
    void testEntityCullingFailOpen()
    {
        /* No crash, no cull when the mod or world is absent */
        assertFalse(LezyEntityCulling.isOccluded(null));
        assertFalse(LezyEntityCulling.isOccludedById(-1));
    }

    @Test
    void testPovFailOpen()
    {
        assertFalse(LezyPov.isSuppressed(null));
    }
}
