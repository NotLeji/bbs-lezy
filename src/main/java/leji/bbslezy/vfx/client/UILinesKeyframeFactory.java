package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;

/**
 * Keyframe editor for the {@code motion_lines} channel.
 */
public class UILinesKeyframeFactory extends UIPoseKeyframeFactory
{
    public UILinesKeyframeFactory(Keyframe<Pose> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
    }
}
