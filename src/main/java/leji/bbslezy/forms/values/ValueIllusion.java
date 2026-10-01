package leji.bbslezy.forms.values;

import leji.bbslezy.forms.utils.Illusion;
import leji.bbslezy.utils.keyframes.factories.IllusionKeyframeFactory;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;

public class ValueIllusion extends BaseKeyframeFactoryValue<Illusion>
{
    public ValueIllusion(String id, Illusion value)
    {
        super(id, IllusionKeyframeFactory.INSTANCE, value);
    }
}
