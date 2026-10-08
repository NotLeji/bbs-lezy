package leji.bbslezy.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LezyPreviewSnapTest
{
    @Test
    void testShouldSnapConditions()
    {
        /* Must snap when setting is true, context is valid, and not controlling */
        assertTrue(LezyPreviewSnap.shouldSnap(true, true, false));

        /* Must not snap if setting disabled */
        assertFalse(LezyPreviewSnap.shouldSnap(false, true, false));

        /* Must not snap if no context */
        assertFalse(LezyPreviewSnap.shouldSnap(true, false, false));

        /* Must not snap if user is actively controlling the camera */
        assertFalse(LezyPreviewSnap.shouldSnap(true, true, true));
    }
}
