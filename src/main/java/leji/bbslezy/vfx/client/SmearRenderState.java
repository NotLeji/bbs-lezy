package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.utils.pose.Pose;

import java.util.Set;

/**
 * Shared state driving per-bone and whole-form smear renders.
 */
public final class SmearRenderState
{
    public static boolean active;
    public static boolean linesPending;
    public static float fraction;
    public static float stretch;
    public static float dissolve;
    public static Object target;

    public static boolean arc;
    public static Set<String> hide;
    public static float arcStretch;
    public static float arcTaper;
    public static Pose presentPose;

    private SmearRenderState()
    {}

    public static void begin(Object renderer, float fraction, float stretch, float dissolve)
    {
        active = true;
        target = renderer;
        SmearRenderState.fraction = fraction;
        SmearRenderState.stretch = stretch;
        SmearRenderState.dissolve = dissolve;
    }

    public static void beginArc(Object renderer, float dissolve, Set<String> hide, float arcStretch, float arcTaper, Pose presentPose)
    {
        active = true;
        arc = true;
        target = renderer;
        SmearRenderState.dissolve = dissolve;
        SmearRenderState.hide = hide;
        SmearRenderState.arcStretch = arcStretch;
        SmearRenderState.arcTaper = arcTaper;
        SmearRenderState.presentPose = presentPose;
        fraction = 0F;
        stretch = 0F;
    }

    public static void end()
    {
        active = false;
        arc = false;
        target = null;
        hide = null;
        fraction = 0F;
        stretch = 0F;
        dissolve = 0F;
        arcStretch = 0F;
        arcTaper = 0F;
        presentPose = null;
    }
}
