package leji.bbslezy.perf.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LezyUIWorldSkipTest
{
    @Test
    public void skipOnlyWhenOpaqueSolidDashboardCovered()
    {
        assertTrue(LezyUIWorldSkip.shouldSkip(true, true, true, 1.0F));
        assertFalse(LezyUIWorldSkip.shouldSkip(true, true, true, 0.999F)); /* tint transluen: world terlihat */
        assertFalse(LezyUIWorldSkip.shouldSkip(true, false, true, 1.0F));  /* bukan dashboard utama (film editor) */
        assertFalse(LezyUIWorldSkip.shouldSkip(false, true, true, 1.0F));  /* tanpa screen terpasang */
        assertFalse(LezyUIWorldSkip.shouldSkip(true, true, false, 1.0F));  /* panel butuh world di belakang */
    }
}
