package leji.bbslezy.ui;

/**
 * DaVinci Resolve-style infinite drag math: pure accumulator for raw pointer
 * deltas while the cursor is hidden (GLFW_CURSOR_DISABLED). Unbounded by
 * design — no edge, no wrap, no clamp.
 */
public class LezyInfiniteDrag
{
    private int accumulatedDx;
    private double lastPhysX;
    private boolean tracking;

    public void begin(double physX)
    {
        this.accumulatedDx = 0;
        this.lastPhysX = physX;
        this.tracking = true;
    }

    /**
     * Feed the current raw physical pointer X each frame. Returns the signed
     * whole-pixel delta since the last call (fractional remainder is kept).
     */
    public int feed(double physX)
    {
        if (!this.tracking)
        {
            this.lastPhysX = physX;

            return 0;
        }

        double delta = physX - this.lastPhysX;
        int whole = (int) delta;

        this.lastPhysX += whole;
        this.accumulatedDx += whole;

        return whole;
    }

    public int getAccumulatedDx()
    {
        return this.accumulatedDx;
    }

    public boolean isTracking()
    {
        return this.tracking;
    }

    public void end()
    {
        this.tracking = false;
        this.accumulatedDx = 0;
    }
}
