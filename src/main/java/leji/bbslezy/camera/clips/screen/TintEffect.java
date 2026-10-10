package leji.bbslezy.camera.clips.screen;

/**
 * Fullscreen color tint overlay (used by cinematic transitions and impact flash).
 */
public class TintEffect implements LayeredEffect
{
    public int layer;
    public int renderOrder;
    public int color; /* ARGB, opacity already baked into alpha */

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
