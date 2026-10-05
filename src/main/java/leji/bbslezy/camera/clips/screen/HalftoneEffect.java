package leji.bbslezy.camera.clips.screen;

public class HalftoneEffect implements LayeredEffect
{
    public static final int MODE_BW = 0;
    public static final int MODE_COLOR = 1;

    public int layer;
    public int renderOrder;

    public int mode = MODE_BW;
    public int inkColor = 0xff000000;
    public float cells = 130F;
    public float highlightThreshold = 0.85F;
    public float dotSoftness = 0.05F;
    public float angle = 45F;

    public void reset()
    {
        this.layer = 0;
        this.renderOrder = 0;
        this.mode = MODE_BW;
        this.inkColor = 0xff000000;
        this.cells = 130F;
        this.highlightThreshold = 0.85F;
        this.dotSoftness = 0.05F;
        this.angle = 45F;
    }

    @Override
    public int layer()
    {
        return this.layer;
    }

    @Override
    public int renderOrder()
    {
        return this.renderOrder;
    }
}
