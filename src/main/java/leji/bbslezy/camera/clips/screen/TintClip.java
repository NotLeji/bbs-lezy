package leji.bbslezy.camera.clips.screen;

import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Accessor for tint overlay effects collected during clip evaluation.
 */
public class TintClip
{
    public static List<TintEffect> getTints(ClipContext context)
    {
        return context.clipData.get("tintEffects", ArrayList::new);
    }
}
