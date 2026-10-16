package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.LezyVfxBlendBoneState;
import leji.bbslezy.vfx.client.SmearRenderState;
import leji.bbslezy.vfx.forms.IModelSmearChannels;
import leji.bbslezy.vfx.forms.ISmearBone;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Folds smear_frames into getPose() and handles deformation/offsets during echo passes.
 */
@Mixin(value = ModelFormRenderer.class, remap = false)
public abstract class ModelFormRendererPoseMixin
{
    @Shadow
    public abstract Form getForm();

    @Inject(method = "applyPhysics", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipPhysicsInCopies(IEntity target, mchorse.bbs_mod.cubic.ModelInstance model, float transition, Matrix4f baseTransform, CallbackInfo ci)
    {
        if (SmearRenderState.active && (Object) this == SmearRenderState.target)
        {
            ci.cancel();
        }
    }

    @Inject(method = "getPose", at = @At("RETURN"), cancellable = true, require = 1)
    private void bbslezy$offsetSmear(CallbackInfoReturnable<Pose> cir)
    {
        Pose pose = cir.getReturnValue();

        if (pose == null)
        {
            return;
        }

        if (this.getForm() instanceof ModelForm && this.getForm() instanceof IModelSmearChannels channels)
        {
            bbslezy$copySmear(pose, channels.bbslezy$smearPose().get());
        }

        if (LezyVfxBlendBoneState.active && !LezyVfxBlendBoneState.hide.isEmpty())
        {
            for (String bone : LezyVfxBlendBoneState.hide)
            {
                pose.getOrCreate(bone).color.a = 0F;
            }
        }

        if (!SmearRenderState.active || SmearRenderState.target != this)
        {
            cir.setReturnValue(pose);
            return;
        }

        if (SmearRenderState.hide != null)
        {
            for (String bone : SmearRenderState.hide)
            {
                pose.getOrCreate(bone).color.a = 0F;
            }
        }

        if (SmearRenderState.arc)
        {
            float s = SmearRenderState.arcStretch;
            float taper = SmearRenderState.arcTaper;

            if (s > 0F)
            {
                float along = 1F + s;
                float across = s > 0F ? 1F / (float) Math.sqrt(along) : 1F;
                float shrink = 1F - 0.12F * taper;

                for (PoseTransform pt : pose.transforms.values())
                {
                    if (((ISmearBone) pt).bbslezy$smearArc() > 0F)
                    {
                        pt.scale.x *= across * shrink;
                        pt.scale.y *= along * shrink;
                        pt.scale.z *= across * shrink;
                    }
                }
            }

            cir.setReturnValue(pose);
            return;
        }

        float f = SmearRenderState.fraction;
        float stretch = Math.abs(f) * SmearRenderState.stretch;

        for (PoseTransform pt : pose.transforms.values())
        {
            ISmearBone smear = (ISmearBone) pt;

            if (!smear.bbslezy$hasSmear())
            {
                continue;
            }

            float sx = smear.bbslezy$smearX();
            float sy = smear.bbslezy$smearY();
            float sz = smear.bbslezy$smearZ();

            pt.translate.add(sx * f, sy * f, sz * f);

            if (stretch > 0F)
            {
                float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);

                if (len > 1e-5F)
                {
                    pt.scale.x *= 1F + stretch * Math.abs(sx) / len;
                    pt.scale.y *= 1F + stretch * Math.abs(sy) / len;
                    pt.scale.z *= 1F + stretch * Math.abs(sz) / len;
                }
            }
        }

        cir.setReturnValue(pose);
    }

    @Unique
    private static void bbslezy$copySmear(Pose target, Pose source)
    {
        for (Map.Entry<String, PoseTransform> entry : source.transforms.entrySet())
        {
            ISmearBone src = (ISmearBone) entry.getValue();

            if (!src.bbslezy$hasSmear() && src.bbslezy$smearCount() == 0F && src.bbslezy$smearFalloff() == 0F && src.bbslezy$smearDissolve() == 0F)
            {
                continue;
            }

            ISmearBone dst = (ISmearBone) target.getOrCreate(entry.getKey());

            dst.bbslezy$setSmear(src.bbslezy$smearX(), src.bbslezy$smearY(), src.bbslezy$smearZ());
            dst.bbslezy$setSmearCount(src.bbslezy$smearCount());
            dst.bbslezy$setSmearFalloff(src.bbslezy$smearFalloff());
            dst.bbslezy$setSmearDissolve(src.bbslezy$smearDissolve());
            dst.bbslezy$setSmearManual(src.bbslezy$smearManual());
            dst.bbslezy$setSmearArc(src.bbslezy$smearArc());
            dst.bbslezy$setSmearTime(src.bbslezy$smearTime());
            dst.bbslezy$setSmearStretch(src.bbslezy$smearStretch());
            dst.bbslezy$setSmearDensity(src.bbslezy$smearDensity());
            dst.bbslezy$setSmearOpacity(src.bbslezy$smearOpacity());
        }
    }

    @Inject(method = "applyPose", at = @At("TAIL"))
    private void bbslezy$mergeSmear(Pose targetPose, Pose pose, CallbackInfo ci)
    {
        for (Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
        {
            ISmearBone overlay = (ISmearBone) entry.getValue();

            if (overlay.bbslezy$hasSmear())
            {
                ISmearBone target = (ISmearBone) targetPose.getOrCreate(entry.getKey());

                target.bbslezy$setSmear(
                    target.bbslezy$smearX() + overlay.bbslezy$smearX(),
                    target.bbslezy$smearY() + overlay.bbslezy$smearY(),
                    target.bbslezy$smearZ() + overlay.bbslezy$smearZ());
            }
        }
    }
}
