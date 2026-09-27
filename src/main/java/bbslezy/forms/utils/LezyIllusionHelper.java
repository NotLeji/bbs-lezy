package bbslezy.forms.utils;

import bbslezy.forms.values.ValueIllusion;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.utils.pose.Transform;

public final class LezyIllusionHelper
{
    public static final String ILLUSION_ID = "bbslezy:illusion";
    public static final String ILLUSION_TRANSFORM_ID = "bbslezy:illusion_transform";
    public static final String LEGACY_ILLUSION_ID = "illusion";
    public static final String LEGACY_ILLUSION_TRANSFORM_ID = "illusion_transform";

    private LezyIllusionHelper()
    {}

    public static ValueIllusion getIllusionValue(Form form)
    {
        if (form == null)
        {
            return null;
        }

        BaseValue value = form.get(ILLUSION_ID);

        if (value instanceof ValueIllusion valueIllusion)
        {
            return valueIllusion;
        }

        BaseValue legacy = form.get(LEGACY_ILLUSION_ID);

        if (legacy instanceof ValueIllusion legacyIllusion)
        {
            return legacyIllusion;
        }

        return null;
    }

    public static Illusion getIllusion(Form form)
    {
        ValueIllusion value = getIllusionValue(form);

        return value != null ? value.get() : null;
    }

    public static ValueTransform getIllusionTransformValue(Form form)
    {
        if (form == null)
        {
            return null;
        }

        BaseValue value = form.get(ILLUSION_TRANSFORM_ID);

        if (value instanceof ValueTransform valueTransform)
        {
            return valueTransform;
        }

        BaseValue legacy = form.get(LEGACY_ILLUSION_TRANSFORM_ID);

        if (legacy instanceof ValueTransform legacyTransform)
        {
            return legacyTransform;
        }

        return null;
    }

    public static Transform getIllusionTransform(Form form)
    {
        ValueTransform value = getIllusionTransformValue(form);

        return value != null ? value.get() : null;
    }

    public static boolean hasIllusion(Form form)
    {
        Illusion illusion = getIllusion(form);

        return illusion != null && illusion.count > 0;
    }
}
