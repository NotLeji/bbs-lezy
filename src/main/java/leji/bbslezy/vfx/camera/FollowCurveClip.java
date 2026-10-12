package leji.bbslezy.vfx.camera;

import mchorse.bbs_mod.camera.clips.modifiers.EntityClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * Camera modifier that rides the camera along a {@link leji.bbslezy.vfx.forms.CurveForm} actor in the
 * scene.
 */
public class FollowCurveClip extends EntityClip
{
    /** Keyframed arc-length offset along the curve, 0 (start) .. 1 (end). */
    public final KeyframeChannel<Double> progress = new KeyframeChannel<>("progress", KeyframeFactories.DOUBLE);
    /** Orient the camera along the curve tangent (rails); off = position only. */
    public final ValueBoolean align = new ValueBoolean("align", true);

    public FollowCurveClip()
    {
        super();

        this.add(this.progress);
        this.add(this.align);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        /* No-op on common side; client subclass handles camera positioning */
    }

    @Override
    protected Clip create()
    {
        return new FollowCurveClip();
    }
}
