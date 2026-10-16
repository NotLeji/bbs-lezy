package leji.bbslezy.vfx.client;

/**
 * Duck interface on {@code ModelFormRenderer} (via {@code ModelFormRendererLinesMixin}) letting the smear
 * render draw the motion lines AFTER all its echo copies.
 */
public interface IMotionLineDrawer
{
    /** Draw the motion lines snapshotted on the crisp pass; no-op if none were deferred. */
    void bbslezy$drawDeferredLines();
}
