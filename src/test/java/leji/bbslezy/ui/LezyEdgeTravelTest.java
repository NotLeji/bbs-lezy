package leji.bbslezy.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LezyEdgeTravelTest
{
    @Test
    public void testGetEdgeDirection()
    {
        int menuWidth = 1920;

        // Inside window boundaries
        assertEquals(0, LezyEdgeTravel.getEdgeDirection(10, menuWidth));
        assertEquals(0, LezyEdgeTravel.getEdgeDirection(960, menuWidth));
        assertEquals(0, LezyEdgeTravel.getEdgeDirection(1914, menuWidth));

        // Touching left edge (<= 5)
        assertEquals(-1, LezyEdgeTravel.getEdgeDirection(5, menuWidth));
        assertEquals(-1, LezyEdgeTravel.getEdgeDirection(0, menuWidth));
        assertEquals(-1, LezyEdgeTravel.getEdgeDirection(-10, menuWidth));

        // Touching right edge (>= 1915)
        assertEquals(1, LezyEdgeTravel.getEdgeDirection(1915, menuWidth));
        assertEquals(1, LezyEdgeTravel.getEdgeDirection(1920, menuWidth));
        assertEquals(1, LezyEdgeTravel.getEdgeDirection(1950, menuWidth));
    }

    @Test
    public void testComputeStepNone()
    {
        assertEquals(0, LezyEdgeTravel.computeStep(0, 0));
        assertEquals(0, LezyEdgeTravel.computeStep(0, 5000));
    }

    @Test
    public void testComputeStepInitial()
    {
        // Initial hold (< 400ms) should be BASE_STEP (2)
        assertEquals(LezyEdgeTravel.BASE_STEP, LezyEdgeTravel.computeStep(1, 0));
        assertEquals(LezyEdgeTravel.BASE_STEP, LezyEdgeTravel.computeStep(1, 200));
        assertEquals(LezyEdgeTravel.BASE_STEP, LezyEdgeTravel.computeStep(1, 400));

        // Negative direction
        assertEquals(-LezyEdgeTravel.BASE_STEP, LezyEdgeTravel.computeStep(-1, 0));
        assertEquals(-LezyEdgeTravel.BASE_STEP, LezyEdgeTravel.computeStep(-1, 350));
    }

    @Test
    public void testComputeStepAcceleration()
    {
        // Intermediate hold (400ms + 500ms = 900ms) should be higher than BASE_STEP
        int midStep = LezyEdgeTravel.computeStep(1, 900);
        assertTrue(midStep > LezyEdgeTravel.BASE_STEP);
        assertTrue(midStep < LezyEdgeTravel.MAX_STEP);

        // Long hold (>= 1400ms) should cap at MAX_STEP (10)
        assertEquals(LezyEdgeTravel.MAX_STEP, LezyEdgeTravel.computeStep(1, 1400));
        assertEquals(LezyEdgeTravel.MAX_STEP, LezyEdgeTravel.computeStep(1, 5000));

        // Long hold negative direction
        assertEquals(-LezyEdgeTravel.MAX_STEP, LezyEdgeTravel.computeStep(-1, 1400));
        assertEquals(-LezyEdgeTravel.MAX_STEP, LezyEdgeTravel.computeStep(-1, 5000));
    }
}
