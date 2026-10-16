package leji.bbslezy.vfx.client;

import leji.bbslezy.vfx.forms.IFormSmear;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.List;

/**
 * Keyframe editor for the whole-form {@code smear} channel (non-model forms).
 */
public class UIFormSmearKeyframeFactory extends UISmearKeyframeFactory
{
    public UIFormSmearKeyframeFactory(Keyframe<Pose> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.poseEditor.setPose(keyframe.getValue(), IFormSmear.SENTINEL);
        this.poseEditor.fillGroups(List.of(IFormSmear.SENTINEL), true);
    }
}
