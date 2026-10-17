package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import leji.bbslezy.vfx.forms.IBlendBone;

/**
 * Adds a per-bone blend (a {@link leji.bbslezy.vfx.forms.BlendMode} index + a 0..1 strength) to BBS's
 * {@link PoseTransform}, woven into every pose op so it serialises / interpolates / animates like the
 * built-in channels (mirrors {@code PoseTransformSmearMixin}). The mode is a discrete category, so it
 * STEPS (holds the "from" keyframe's value) instead of tweening through other modes; the factor lerps
 * smoothly.
 */
@Mixin(value = PoseTransform.class, remap = false)
public abstract class PoseTransformBlendMixin implements IBlendBone
{
    @Unique private int bbslezy$bMode;
    @Unique private float bbslezy$bFactor = 1F;

    @Override
    public int bbslezy$blendMode()
    {
        return this.bbslezy$bMode;
    }

    @Override
    public void bbslezy$setBlendMode(int mode)
    {
        this.bbslezy$bMode = mode;
    }

    @Override
    public float bbslezy$blendFactor()
    {
        return this.bbslezy$bFactor;
    }

    @Override
    public void bbslezy$setBlendFactor(float factor)
    {
        this.bbslezy$bFactor = factor;
    }

    @Inject(method = "identity", at = @At("TAIL"))
    private void bbslezy$identity(CallbackInfo ci)
    {
        this.bbslezy$bMode = 0;
        this.bbslezy$bFactor = 1F;
    }

    @Inject(method = "lerp(Lmchorse/bbs_mod/utils/pose/Transform;F)V", at = @At("TAIL"))
    private void bbslezy$lerp(Transform transform, float a, CallbackInfo ci)
    {
        if (transform instanceof IBlendBone o)
        {
            this.bbslezy$bMode = a < 1F ? this.bbslezy$bMode : o.bbslezy$blendMode();
            this.bbslezy$bFactor = Lerps.lerp(this.bbslezy$bFactor, o.bbslezy$blendFactor(), a);
        }
    }

    @Inject(method = "lerp(Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/interps/IInterp;F)V", at = @At("TAIL"))
    private void bbslezy$lerpKeyframes(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x, CallbackInfo ci)
    {
        if (a instanceof IBlendBone a1 && b instanceof IBlendBone b1)
        {
            /* Mode steps (hold the left keyframe); factor interpolates. */
            this.bbslezy$bMode = a1.bbslezy$blendMode();
            this.bbslezy$bFactor = (float) interp.interpolate(IInterp.context.set(a1.bbslezy$blendFactor(), a1.bbslezy$blendFactor(), b1.bbslezy$blendFactor(), b1.bbslezy$blendFactor(), x));
        }
    }

    @Inject(method = "autoLerp", at = @At("TAIL"))
    private void bbslezy$autoLerp(Transform preA, Transform a, Transform b, Transform postB, float pt, float at, float bt, float qt, boolean clamped, float x, CallbackInfo ci)
    {
        if (a instanceof IBlendBone a1)
        {
            this.bbslezy$bMode = a1.bbslezy$blendMode();
            this.bbslezy$bFactor = a instanceof IBlendBone && b instanceof IBlendBone b1
                ? Lerps.lerp(a1.bbslezy$blendFactor(), b1.bbslezy$blendFactor(), x)
                : a1.bbslezy$blendFactor();
        }
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void bbslezy$equals(Object obj, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue() && obj instanceof IBlendBone o)
        {
            boolean matches = this.bbslezy$bMode == o.bbslezy$blendMode() && this.bbslezy$bFactor == o.bbslezy$blendFactor();

            if (!matches)
            {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "copy(Lmchorse/bbs_mod/utils/pose/Transform;)V", at = @At("TAIL"))
    private void bbslezy$copy(Transform transform, CallbackInfo ci)
    {
        if (transform instanceof IBlendBone o)
        {
            this.bbslezy$bMode = o.bbslezy$blendMode();
            this.bbslezy$bFactor = o.bbslezy$blendFactor();
        }
    }

    @Inject(method = "toData", at = @At("TAIL"))
    private void bbslezy$toData(MapType data, CallbackInfo ci)
    {
        if (this.bbslezy$bMode != 0 || this.bbslezy$bFactor != 1F)
        {
            /* LEGACY DATA KEYS: "xavin_blend_*" predate the xavin → bbsvfx rename; they are written
             * into saved poses, so the names must stay as-is for old scenes/films to load. */
            data.putInt("xavin_blend_mode", this.bbslezy$bMode);
            data.putFloat("xavin_blend_factor", this.bbslezy$bFactor);
        }
    }

    @Inject(method = "fromData", at = @At("TAIL"))
    private void bbslezy$fromData(MapType data, CallbackInfo ci)
    {
        this.bbslezy$bMode = data.has("xavin_blend_mode") ? data.getInt("xavin_blend_mode") : 0;
        this.bbslezy$bFactor = data.has("xavin_blend_factor") ? data.getFloat("xavin_blend_factor") : 1F;
    }
}
