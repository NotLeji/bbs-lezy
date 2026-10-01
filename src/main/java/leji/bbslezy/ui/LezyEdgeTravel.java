package leji.bbslezy.ui;

public class LezyEdgeTravel
{
    public static final int BORDER = 5;
    public static final int BASE_STEP = 2;
    public static final int MAX_STEP = 10;
    public static final long ACCEL_DELAY_MS = 400;

    /**
     * Determines whether the cursor is touching the screen edge.
     *
     * @param mouseX global mouse X coordinate
     * @param menuWidth width of current menu/window
     * @return -1 if touching left edge, 1 if touching right edge, 0 if inside
     */
    public static int getEdgeDirection(int mouseX, int menuWidth)
    {
        if (mouseX <= BORDER)
        {
            return -1;
        }
        else if (mouseX >= menuWidth - BORDER)
        {
            return 1;
        }

        return 0;
    }

    /**
     * Computes the step amount for one frame of edge travel based on how long
     * the mouse has been held continuously at the edge.
     *
     * @param direction -1 (left), 1 (right), or 0 (none)
     * @param edgeHoldDurationMs duration in milliseconds mouse has been held at edge
     * @return signed delta to apply to the accumulated offset/shift
     */
    public static int computeStep(int direction, long edgeHoldDurationMs)
    {
        if (direction == 0)
        {
            return 0;
        }

        int step = BASE_STEP;

        if (edgeHoldDurationMs > ACCEL_DELAY_MS)
        {
            long excess = edgeHoldDurationMs - ACCEL_DELAY_MS;
            step += (int) Math.min(MAX_STEP - BASE_STEP, excess * (MAX_STEP - BASE_STEP) / 1000L);
        }

        return direction * step;
    }
}
