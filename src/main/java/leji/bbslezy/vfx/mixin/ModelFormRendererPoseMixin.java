package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.LezyVfxBlendBoneState;
import leji.bbslezy.vfx.client.LezyImpactSilhouette;
import leji.bbslezy.vfx.client.SmearRenderState;
import mchorse.bbs_mod.cubic.ModelInstance;
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

    /**
     * A pass that needs a DIFFERENT pose than the frame's base pass must not take the animator
     * channel cache. The cached path ({@code ModelInstance#restoreChannels}) returns BEFORE
     * {@code applyPose(this.getPose())}, so {@code getPose()} is never called on a hit and the
     * per-bone hide/offset written there is lost. Two passes need that:
     *
     * <ul>
     *   <li><b>Smear copies</b> — re-posed at earlier film times via {@code applyProperties}, which
     *       writes a runtime value silently so the form's pose version never changes; the token
     *       (form, entity, transition, epoch, pose version) matched the base pass and every copy got
     *       the crisp channels, i.e. pixel-identical copies (the smear was invisible).</li>
     *   <li><b>Per-bone blend passes</b> — the offscreen group pass isolates its bones by setting
     *       {@code LezyVfxBlendBoneState.hide} and hiding through the pose colour alpha in
     *       {@code getPose()}; a cache hit skipped that, so the WHOLE model rendered into the group
     *       buffer and the blend leaked onto bones that were not selected.</li>
     * </ul>
     *
     * <p>Forcing a fresh evaluation while either pass is active for this very renderer leaves
     * ordinary renders on the per-frame channel cache.</p>
     */
    @Inject(method = "evaluateChannels", at = @At("HEAD"))
    private void bbslezy$freshChannelsForCopies(IEntity entity, ModelInstance model, float transition, CallbackInfo ci)
    {
        if (model == null)
        {
            return;
        }

        boolean smearPass = SmearRenderState.active && (Object) this == SmearRenderState.target;

        if (smearPass || LezyVfxBlendBoneState.active || LezyImpactSilhouette.replaying)
        {
            model.clearChannels();
        }
    }

    @Inject(method = "applyPhysics", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipPhysicsInCopies(IEntity target, ModelInstance model, float transition, Matrix4f baseTransform, CallbackInfo ci)
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

        if (LezyImpactSilhouette.replaying)
        {
            for (PoseTransform pt : pose.transforms.values())
            {
                pt.color.a = 1F;
            }
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
