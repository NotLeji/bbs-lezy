package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIFloatKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Keyframe editor for the 'destruction' master track: strictly limits keyframe value between 0.0 and 1.0.
 */
public class UIDestructionKeyframeFactory extends UIFloatKeyframeFactory
{
    public UIDestructionKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
        this.value.limit(0F, 1F);
    }

    @Override
    protected void setKeyframeValue(Keyframe<Float> keyframe, double value)
    {
        float clamped = (float) Math.max(0.0, Math.min(1.0, value));
        keyframe.setValue(clamped);
    }
}
