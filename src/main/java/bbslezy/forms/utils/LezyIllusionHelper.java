package bbslezy.forms.utils;

import bbslezy.forms.values.ValueIllusion;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.replays.Replay;
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

        /* Check "illusion" first (standard in CML & keyframe tracks), then namespaced */
        BaseValue legacy = form.get(LEGACY_ILLUSION_ID);

        if (legacy instanceof ValueIllusion legacyIllusion)
        {
            return legacyIllusion;
        }

        BaseValue value = form.get(ILLUSION_ID);

        if (value instanceof ValueIllusion valueIllusion)
        {
            return valueIllusion;
        }

        return null;
    }

    public static Illusion getIllusion(Form form)
    {
        ValueIllusion value = getIllusionValue(form);

        return value != null ? value.get() : null;
    }

    /**
     * Resolves illusion from form, and if absent or count == 0 on a replay entity,
     * seamlessly falls back to the owning Replay's source form and syncs it.
     */
    public static Replay getCurrentReplay()
    {
        try
        {
            return FilmControllerContext.instance.replay;
        }
        catch (Throwable ignored)
        {
            return null;
        }
    }

    public static Illusion resolveIllusion(Form form)
    {
        Illusion illusion = getIllusion(form);

        if (illusion != null && illusion.count > 0)
        {
            return illusion;
        }

        Replay currentReplay = getCurrentReplay();

        if (currentReplay != null)
        {
            Form replayForm = currentReplay.form.get();

            if (replayForm != null && replayForm != form)
            {
                Illusion fromReplay = getIllusion(replayForm);

                if (fromReplay != null && fromReplay.count > 0)
                {
                    syncIllusion(replayForm, form);

                    return fromReplay;
                }
            }
        }

        return illusion;
    }

    public static ValueTransform getIllusionTransformValue(Form form)
    {
        if (form == null)
        {
            return null;
        }

        BaseValue legacy = form.get(LEGACY_ILLUSION_TRANSFORM_ID);

        if (legacy instanceof ValueTransform legacyTransform)
        {
            return legacyTransform;
        }

        BaseValue value = form.get(ILLUSION_TRANSFORM_ID);

        if (value instanceof ValueTransform valueTransform)
        {
            return valueTransform;
        }

        return null;
    }

    public static Transform getIllusionTransform(Form form)
    {
        ValueTransform value = getIllusionTransformValue(form);

        if (value != null)
        {
            return value.get();
        }

        Replay currentReplay = getCurrentReplay();

        if (currentReplay != null)
        {
            Form replayForm = currentReplay.form.get();

            if (replayForm != null && replayForm != form)
            {
                return getIllusionTransform(replayForm);
            }
        }
        return null;
    }

    public static void syncIllusion(Form source, Form target)
    {
        if (source == null || target == null)
        {
            return;
        }

        Illusion illusion = getIllusion(source);

        if (illusion != null)
        {
            setIllusion(target, illusion);
        }

        Transform transform = getIllusionTransform(source);

        if (transform != null)
        {
            setIllusionTransform(target, transform);
        }
    }

    public static void setIllusion(Form form, Illusion illusion)
    {
        if (form == null || illusion == null)
        {
            return;
        }

        ValueIllusion val1 = getIllusionValue(form);

        if (val1 == null)
        {
            val1 = new ValueIllusion(LEGACY_ILLUSION_ID, illusion.copy());
            form.add(val1);
        }
        else
        {
            val1.set(illusion.copy());
        }

        BaseValue val2 = form.get(ILLUSION_ID);

        if (val2 instanceof ValueIllusion namespaced)
        {
            namespaced.set(illusion.copy());
        }
        else if (val2 == null)
        {
            form.add(new ValueIllusion(ILLUSION_ID, illusion.copy()));
        }
    }

    public static void setIllusionTransform(Form form, Transform transform)
    {
        if (form == null || transform == null)
        {
            return;
        }

        ValueTransform val1 = getIllusionTransformValue(form);

        if (val1 == null)
        {
            val1 = new ValueTransform(LEGACY_ILLUSION_TRANSFORM_ID, new Transform());
            val1.set(transform.copy());
            form.add(val1);
        }
        else
        {
            val1.set(transform.copy());
        }

        BaseValue val2 = form.get(ILLUSION_TRANSFORM_ID);

        if (val2 instanceof ValueTransform namespaced)
        {
            namespaced.set(transform.copy());
        }
        else if (val2 == null)
        {
            ValueTransform t = new ValueTransform(ILLUSION_TRANSFORM_ID, new Transform());
            t.set(transform.copy());
            form.add(t);
        }
    }

    public static boolean hasIllusion(Form form)
    {
        Illusion illusion = resolveIllusion(form);

        return illusion != null && illusion.count > 0;
    }
}
