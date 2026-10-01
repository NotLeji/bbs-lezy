package leji.bbslezy.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LezyInfiniteDragTest
{
    @Test
    public void testBeginResetsAccumulator()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();
        drag.begin(100.0);
        assertEquals(0, drag.getAccumulatedDx());
        assertTrue(drag.isTracking());
    }

    @Test
    public void testFeedAccumulatesWholePixels()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();
        drag.begin(100.0);

        assertEquals(5, drag.feed(105.0));
        assertEquals(5, drag.getAccumulatedDx());

        assertEquals(-3, drag.feed(102.0));
        assertEquals(2, drag.getAccumulatedDx());
    }

    @Test
    public void testFeedKeepsFractionalRemainder()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();
        drag.begin(100.0);

        assertEquals(0, drag.feed(100.7));
        assertEquals(0, drag.getAccumulatedDx());
        assertEquals(1, drag.feed(101.2));
        assertEquals(1, drag.getAccumulatedDx());
    }

    @Test
    public void testFeedWithoutTrackingReturnsZero()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();

        assertEquals(0, drag.feed(500.0));
        assertEquals(0, drag.getAccumulatedDx());
    }

    @Test
    public void testEndStopsTrackingAndResets()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();
        drag.begin(100.0);
        drag.feed(150.0);
        drag.end();

        assertFalse(drag.isTracking());
        assertEquals(0, drag.getAccumulatedDx());
        assertEquals(0, drag.feed(200.0));
    }

    @Test
    public void testUnboundedTravelBothDirections()
    {
        LezyInfiniteDrag drag = new LezyInfiniteDrag();
        drag.begin(0.0);

        for (int i = 0; i < 100; i++)
        {
            drag.feed((i + 1) * 50.0);
        }

        assertEquals(5000, drag.getAccumulatedDx());

        for (int i = 0; i < 200; i++)
        {
            drag.feed(5000.0 - (i + 1) * 50.0);
        }

        assertEquals(-5000, drag.getAccumulatedDx());
    }
}
