package leji.bbslezy.vfx.client;

import java.util.Collections;
import java.util.Set;

/**
 * Active set of bones to HIDE during a model render, used to isolate bones for the per-bone blend: in the
 * in-world pass the blended bones are hidden (so the rest render normally), and in each offscreen blend
 * pass everything EXCEPT that group's bones is hidden (so only the group renders, to be composited with
 * its mode). Read by {@code ModelFormRendererPoseMixin.getPose} (via the bone colour alpha).
 */
public final class LezyVfxBlendBoneState
{
    public static boolean active;
    public static Set<String> hide = Collections.emptySet();

    private LezyVfxBlendBoneState()
    {}

    public static void begin(Set<String> hide)
    {
        LezyVfxBlendBoneState.active = true;
        LezyVfxBlendBoneState.hide = hide;
    }

    public static void end()
    {
        LezyVfxBlendBoneState.active = false;
        LezyVfxBlendBoneState.hide = Collections.emptySet();
    }
}
