package leji.bbslezy.vfx.forms;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

/**
 * One control point of a {@link CurveForm}.
 */
public class CurvePoint extends ValueGroup
{
    /** Stable name of this point's bone in the form's pose (where its animatable position is stored). */
    public final ValueString bone = new ValueString("bone", "");
    public final ValueFloat width = new ValueFloat("width", 0.2F, 0.001F, Float.POSITIVE_INFINITY);

    public CurvePoint(String id)
    {
        super(id);

        this.add(this.bone);
        this.add(this.width);
    }
}
