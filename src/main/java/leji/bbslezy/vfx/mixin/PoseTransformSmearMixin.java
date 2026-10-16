package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.forms.ISmearBone;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a per-bone smear vector to BBS's {@link PoseTransform}.
 */
@Mixin(value = PoseTransform.class, remap = false)
public abstract class PoseTransformSmearMixin implements ISmearBone
{
    @Unique private float bbslezy$sx;
    @Unique private float bbslezy$sy;
    @Unique private float bbslezy$sz;
    @Unique private float bbslezy$count;
    @Unique private float bbslezy$falloff;
    @Unique private float bbslezy$dissolve;
    @Unique private float bbslezy$manual;
    @Unique private float bbslezy$arc;
    @Unique private float bbslezy$time;
    @Unique private float bbslezy$stretch;
    @Unique private float bbslezy$density;
    @Unique private float bbslezy$opacity;
    @Unique private float bbslezy$lines;
    @Unique private float bbslezy$linesCount;
    @Unique private float bbslezy$linesWidth;
    @Unique private float bbslezy$linesSpread;
    @Unique private float bbslezy$lox;
    @Unique private float bbslezy$loy;
    @Unique private float bbslezy$loz;
    @Unique private float bbslezy$linesTexture;

    @Override
    public float bbslezy$smearX()
    {
        return this.bbslezy$sx;
    }

    @Override
    public float bbslezy$smearY()
    {
        return this.bbslezy$sy;
    }

    @Override
    public float bbslezy$smearZ()
    {
        return this.bbslezy$sz;
    }

    @Override
    public void bbslezy$setSmear(float x, float y, float z)
    {
        this.bbslezy$sx = x;
        this.bbslezy$sy = y;
        this.bbslezy$sz = z;
    }

    @Override
    public float bbslezy$smearCount()
    {
        return this.bbslezy$count;
    }

    @Override
    public void bbslezy$setSmearCount(float count)
    {
        this.bbslezy$count = count;
    }

    @Override
    public float bbslezy$smearFalloff()
    {
        return this.bbslezy$falloff;
    }

    @Override
    public void bbslezy$setSmearFalloff(float falloff)
    {
        this.bbslezy$falloff = falloff;
    }

    @Override
    public float bbslezy$smearDissolve()
    {
        return this.bbslezy$dissolve;
    }

    @Override
    public void bbslezy$setSmearDissolve(float dissolve)
    {
        this.bbslezy$dissolve = dissolve;
    }

    @Override
    public float bbslezy$smearManual()
    {
        return this.bbslezy$manual;
    }

    @Override
    public void bbslezy$setSmearManual(float manual)
    {
        this.bbslezy$manual = manual;
    }

    @Override
    public float bbslezy$smearArc()
    {
        return this.bbslezy$arc;
    }

    @Override
    public void bbslezy$setSmearArc(float arc)
    {
        this.bbslezy$arc = arc;
    }

    @Override
    public float bbslezy$smearTime()
    {
        return this.bbslezy$time;
    }

    @Override
    public void bbslezy$setSmearTime(float time)
    {
        this.bbslezy$time = time;
    }

    @Override
    public float bbslezy$smearStretch()
    {
        return this.bbslezy$stretch;
    }

    @Override
    public void bbslezy$setSmearStretch(float stretch)
    {
        this.bbslezy$stretch = stretch;
    }

    @Override
    public float bbslezy$smearDensity()
    {
        return this.bbslezy$density;
    }

    @Override
    public void bbslezy$setSmearDensity(float density)
    {
        this.bbslezy$density = density;
    }

    @Override
    public float bbslezy$smearOpacity()
    {
        return this.bbslezy$opacity;
    }

    @Override
    public void bbslezy$setSmearOpacity(float opacity)
    {
        this.bbslezy$opacity = opacity;
    }

    @Override
    public float bbslezy$smearLines()
    {
        return this.bbslezy$lines;
    }

    @Override
    public void bbslezy$setSmearLines(float lines)
    {
        this.bbslezy$lines = lines;
    }

    @Override
    public float bbslezy$smearLinesCount()
    {
        return this.bbslezy$linesCount;
    }

    @Override
    public void bbslezy$setSmearLinesCount(float count)
    {
        this.bbslezy$linesCount = count;
    }

    @Override
    public float bbslezy$smearLinesWidth()
    {
        return this.bbslezy$linesWidth;
    }

    @Override
    public void bbslezy$setSmearLinesWidth(float width)
    {
        this.bbslezy$linesWidth = width;
    }

    @Override
    public float bbslezy$smearLinesSpread()
    {
        return this.bbslezy$linesSpread;
    }

    @Override
    public void bbslezy$setSmearLinesSpread(float spread)
    {
        this.bbslezy$linesSpread = spread;
    }

    @Override
    public float bbslezy$smearLinesOffsetX()
    {
        return this.bbslezy$lox;
    }

    @Override
    public float bbslezy$smearLinesOffsetY()
    {
        return this.bbslezy$loy;
    }

    @Override
    public float bbslezy$smearLinesOffsetZ()
    {
        return this.bbslezy$loz;
    }

    @Override
    public void bbslezy$setSmearLinesOffset(float x, float y, float z)
    {
        this.bbslezy$lox = x;
        this.bbslezy$loy = y;
        this.bbslezy$loz = z;
    }

    @Override
    public float bbslezy$smearLinesTexture()
    {
        return this.bbslezy$linesTexture;
    }

    @Override
    public void bbslezy$setSmearLinesTexture(float texture)
    {
        this.bbslezy$linesTexture = texture;
    }

    @Inject(method = "identity", at = @At("TAIL"))
    private void bbslezy$identity(CallbackInfo ci)
    {
        this.bbslezy$sx = this.bbslezy$sy = this.bbslezy$sz = 0F;
        this.bbslezy$count = this.bbslezy$falloff = this.bbslezy$dissolve = 0F;
        this.bbslezy$manual = this.bbslezy$arc = this.bbslezy$time = this.bbslezy$stretch = this.bbslezy$density = this.bbslezy$opacity = 0F;
        this.bbslezy$lines = this.bbslezy$linesCount = this.bbslezy$linesWidth = this.bbslezy$linesSpread = 0F;
        this.bbslezy$lox = this.bbslezy$loy = this.bbslezy$loz = 0F;
        this.bbslezy$linesTexture = 0F;
    }

    @Unique
    private void bbslezy$stepFrom(ISmearBone o)
    {
        this.bbslezy$sx = o.bbslezy$smearX();
        this.bbslezy$sy = o.bbslezy$smearY();
        this.bbslezy$sz = o.bbslezy$smearZ();
        this.bbslezy$count = o.bbslezy$smearCount();
        this.bbslezy$falloff = o.bbslezy$smearFalloff();
        this.bbslezy$dissolve = o.bbslezy$smearDissolve();
        this.bbslezy$manual = o.bbslezy$smearManual();
        this.bbslezy$arc = o.bbslezy$smearArc();
        this.bbslezy$time = o.bbslezy$smearTime();
        this.bbslezy$stretch = o.bbslezy$smearStretch();
        this.bbslezy$density = o.bbslezy$smearDensity();
        this.bbslezy$opacity = o.bbslezy$smearOpacity();
        this.bbslezy$lines = o.bbslezy$smearLines();
        this.bbslezy$linesCount = o.bbslezy$smearLinesCount();
        this.bbslezy$linesWidth = o.bbslezy$smearLinesWidth();
        this.bbslezy$linesSpread = o.bbslezy$smearLinesSpread();
        this.bbslezy$lox = o.bbslezy$smearLinesOffsetX();
        this.bbslezy$loy = o.bbslezy$smearLinesOffsetY();
        this.bbslezy$loz = o.bbslezy$smearLinesOffsetZ();
        this.bbslezy$linesTexture = o.bbslezy$smearLinesTexture();
    }

    @Inject(method = "lerp(Lmchorse/bbs_mod/utils/pose/Transform;F)V", at = @At("TAIL"))
    private void bbslezy$lerp(Transform transform, float a, CallbackInfo ci)
    {
        if (transform instanceof ISmearBone o && a >= 0.5F)
        {
            this.bbslezy$stepFrom(o);
        }
    }

    @Inject(method = "lerp(Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/pose/Transform;Lmchorse/bbs_mod/utils/interps/IInterp;F)V", at = @At("TAIL"))
    private void bbslezy$lerpKeyframes(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x, CallbackInfo ci)
    {
        if (a instanceof ISmearBone a1)
        {
            this.bbslezy$stepFrom(a1);
        }
    }

    @Inject(method = "autoLerp", at = @At("TAIL"))
    private void bbslezy$autoLerp(Transform preA, Transform a, Transform b, Transform postB, float pt, float at, float bt, float qt, boolean clamped, float x, CallbackInfo ci)
    {
        if (a instanceof ISmearBone a1)
        {
            this.bbslezy$stepFrom(a1);
        }
    }

    @Inject(method = "equals", at = @At("RETURN"), cancellable = true)
    private void bbslezy$equals(Object obj, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue() && obj instanceof ISmearBone o)
        {
            boolean matches = this.bbslezy$sx == o.bbslezy$smearX() && this.bbslezy$sy == o.bbslezy$smearY() && this.bbslezy$sz == o.bbslezy$smearZ()
                && this.bbslezy$count == o.bbslezy$smearCount() && this.bbslezy$falloff == o.bbslezy$smearFalloff()
                && this.bbslezy$dissolve == o.bbslezy$smearDissolve()
                && this.bbslezy$manual == o.bbslezy$smearManual()
                && this.bbslezy$arc == o.bbslezy$smearArc() && this.bbslezy$time == o.bbslezy$smearTime()
                && this.bbslezy$stretch == o.bbslezy$smearStretch() && this.bbslezy$density == o.bbslezy$smearDensity()
                && this.bbslezy$opacity == o.bbslezy$smearOpacity()
                && this.bbslezy$lines == o.bbslezy$smearLines() && this.bbslezy$linesCount == o.bbslezy$smearLinesCount()
                && this.bbslezy$linesWidth == o.bbslezy$smearLinesWidth() && this.bbslezy$linesSpread == o.bbslezy$smearLinesSpread()
                && this.bbslezy$lox == o.bbslezy$smearLinesOffsetX() && this.bbslezy$loy == o.bbslezy$smearLinesOffsetY() && this.bbslezy$loz == o.bbslezy$smearLinesOffsetZ()
                && this.bbslezy$linesTexture == o.bbslezy$smearLinesTexture();

            if (!matches)
            {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "copy(Lmchorse/bbs_mod/utils/pose/Transform;)V", at = @At("TAIL"))
    private void bbslezy$copy(Transform transform, CallbackInfo ci)
    {
        if (transform instanceof ISmearBone o)
        {
            this.bbslezy$sx = o.bbslezy$smearX();
            this.bbslezy$sy = o.bbslezy$smearY();
            this.bbslezy$sz = o.bbslezy$smearZ();
            this.bbslezy$count = o.bbslezy$smearCount();
            this.bbslezy$falloff = o.bbslezy$smearFalloff();
            this.bbslezy$dissolve = o.bbslezy$smearDissolve();
            this.bbslezy$manual = o.bbslezy$smearManual();
            this.bbslezy$arc = o.bbslezy$smearArc();
            this.bbslezy$time = o.bbslezy$smearTime();
            this.bbslezy$stretch = o.bbslezy$smearStretch();
            this.bbslezy$density = o.bbslezy$smearDensity();
            this.bbslezy$opacity = o.bbslezy$smearOpacity();
            this.bbslezy$lines = o.bbslezy$smearLines();
            this.bbslezy$linesCount = o.bbslezy$smearLinesCount();
            this.bbslezy$linesWidth = o.bbslezy$smearLinesWidth();
            this.bbslezy$linesSpread = o.bbslezy$smearLinesSpread();
            this.bbslezy$lox = o.bbslezy$smearLinesOffsetX();
            this.bbslezy$loy = o.bbslezy$smearLinesOffsetY();
            this.bbslezy$loz = o.bbslezy$smearLinesOffsetZ();
            this.bbslezy$linesTexture = o.bbslezy$smearLinesTexture();
        }
    }

    @Inject(method = "add", at = @At("TAIL"))
    private void bbslezy$add(Transform transform, CallbackInfo ci)
    {
        if (transform instanceof ISmearBone o)
        {
            this.bbslezy$sx += o.bbslezy$smearX();
            this.bbslezy$sy += o.bbslezy$smearY();
            this.bbslezy$sz += o.bbslezy$smearZ();
            this.bbslezy$count += o.bbslezy$smearCount();
            this.bbslezy$falloff += o.bbslezy$smearFalloff();
            this.bbslezy$dissolve += o.bbslezy$smearDissolve();
            this.bbslezy$manual += o.bbslezy$smearManual();
            this.bbslezy$arc += o.bbslezy$smearArc();
            this.bbslezy$time += o.bbslezy$smearTime();
            this.bbslezy$stretch += o.bbslezy$smearStretch();
            this.bbslezy$density += o.bbslezy$smearDensity();
            this.bbslezy$opacity += o.bbslezy$smearOpacity();
            this.bbslezy$lines += o.bbslezy$smearLines();
            this.bbslezy$linesCount += o.bbslezy$smearLinesCount();
            this.bbslezy$linesWidth += o.bbslezy$smearLinesWidth();
            this.bbslezy$linesSpread += o.bbslezy$smearLinesSpread();
            this.bbslezy$lox += o.bbslezy$smearLinesOffsetX();
            this.bbslezy$loy += o.bbslezy$smearLinesOffsetY();
            this.bbslezy$loz += o.bbslezy$smearLinesOffsetZ();
            this.bbslezy$linesTexture += o.bbslezy$smearLinesTexture();
        }
    }

    @Inject(method = "toData", at = @At("TAIL"))
    private void bbslezy$toData(MapType data, CallbackInfo ci)
    {
        if (this.bbslezy$sx != 0F || this.bbslezy$sy != 0F || this.bbslezy$sz != 0F
            || this.bbslezy$count != 0F || this.bbslezy$falloff != 0F || this.bbslezy$dissolve != 0F
            || this.bbslezy$manual != 0F
            || this.bbslezy$arc != 0F || this.bbslezy$time != 0F || this.bbslezy$stretch != 0F || this.bbslezy$density != 0F || this.bbslezy$opacity != 0F
            || this.bbslezy$lines != 0F || this.bbslezy$linesCount != 0F || this.bbslezy$linesWidth != 0F || this.bbslezy$linesSpread != 0F
            || this.bbslezy$lox != 0F || this.bbslezy$loy != 0F || this.bbslezy$loz != 0F
            || this.bbslezy$linesTexture != 0F)
        {
            /* LEGACY DATA KEYS: preserved for saved scenes/films. */
            data.putFloat("xavin_smear_x", this.bbslezy$sx);
            data.putFloat("xavin_smear_y", this.bbslezy$sy);
            data.putFloat("xavin_smear_z", this.bbslezy$sz);
            data.putFloat("xavin_smear_count", this.bbslezy$count);
            data.putFloat("xavin_smear_falloff", this.bbslezy$falloff);
            data.putFloat("xavin_smear_dissolve", this.bbslezy$dissolve);
            data.putFloat("xavin_smear_manual", this.bbslezy$manual);
            data.putFloat("xavin_smear_arc", this.bbslezy$arc);
            data.putFloat("xavin_smear_time", this.bbslezy$time);
            data.putFloat("xavin_smear_stretch", this.bbslezy$stretch);
            data.putFloat("xavin_smear_density", this.bbslezy$density);
            data.putFloat("xavin_smear_opacity", this.bbslezy$opacity);
            data.putFloat("xavin_smear_lines", this.bbslezy$lines);
            data.putFloat("xavin_smear_lines_count", this.bbslezy$linesCount);
            data.putFloat("xavin_smear_lines_width", this.bbslezy$linesWidth);
            data.putFloat("xavin_smear_lines_spread", this.bbslezy$linesSpread);
            data.putFloat("xavin_smear_lines_ox", this.bbslezy$lox);
            data.putFloat("xavin_smear_lines_oy", this.bbslezy$loy);
            data.putFloat("xavin_smear_lines_oz", this.bbslezy$loz);
            data.putFloat("xavin_smear_lines_texture", this.bbslezy$linesTexture);
        }
    }

    @Inject(method = "fromData", at = @At("TAIL"))
    private void bbslezy$fromData(MapType data, CallbackInfo ci)
    {
        this.bbslezy$sx = data.getFloat("xavin_smear_x");
        this.bbslezy$sy = data.getFloat("xavin_smear_y");
        this.bbslezy$sz = data.getFloat("xavin_smear_z");
        this.bbslezy$count = data.getFloat("xavin_smear_count");
        this.bbslezy$falloff = data.getFloat("xavin_smear_falloff");
        this.bbslezy$dissolve = data.getFloat("xavin_smear_dissolve");
        this.bbslezy$manual = data.getFloat("xavin_smear_manual");
        this.bbslezy$arc = data.getFloat("xavin_smear_arc");
        this.bbslezy$time = data.getFloat("xavin_smear_time");
        this.bbslezy$stretch = data.getFloat("xavin_smear_stretch");
        this.bbslezy$density = data.getFloat("xavin_smear_density");
        this.bbslezy$opacity = data.getFloat("xavin_smear_opacity");
        this.bbslezy$lines = data.getFloat("xavin_smear_lines");
        this.bbslezy$linesCount = data.getFloat("xavin_smear_lines_count");
        this.bbslezy$linesWidth = data.getFloat("xavin_smear_lines_width");
        this.bbslezy$linesSpread = data.getFloat("xavin_smear_lines_spread");
        this.bbslezy$lox = data.getFloat("xavin_smear_lines_ox");
        this.bbslezy$loy = data.getFloat("xavin_smear_lines_oy");
        this.bbslezy$loz = data.getFloat("xavin_smear_lines_oz");
        this.bbslezy$linesTexture = data.getFloat("xavin_smear_lines_texture");
    }
}
