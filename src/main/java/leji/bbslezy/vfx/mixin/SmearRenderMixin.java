package leji.bbslezy.vfx.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import leji.bbslezy.vfx.client.BbsVfxArcTrail;
import leji.bbslezy.vfx.client.IMotionLineDrawer;
import leji.bbslezy.vfx.client.LezyImpactSilhouette;
import leji.bbslezy.vfx.client.SmearRenderState;
import leji.bbslezy.vfx.client.SmearReplayState;
import leji.bbslezy.vfx.forms.IFormSmear;
import leji.bbslezy.vfx.forms.ISmearBone;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-bone smear render hook wrapping FormUtilsClient.render.
 */
@Mixin(value = FormUtilsClient.class, remap = false)
public abstract class SmearRenderMixin
{
    @Redirect(
        method = "render(Lmchorse/bbs_mod/forms/forms/Form;Lmchorse/bbs_mod/forms/renderers/FormRenderingContext;)V",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/renderers/FormRenderer;render(Lmchorse/bbs_mod/forms/renderers/FormRenderingContext;)V"))
    private static void bbslezy$smear(FormRenderer renderer, FormRenderingContext context, Form form)
    {
        if (context.stencilMap != null || SmearRenderState.active || LezyImpactSilhouette.replaying)
        {
            renderer.render(context);
            return;
        }

        if (!(renderer instanceof ModelFormRenderer model))
        {
            if (form instanceof IFormSmear fs)
            {
                Pose fp = fs.bbslezy$formSmearPose();
                ISmearBone smear = fp == null ? null : (ISmearBone) fp.get(IFormSmear.SENTINEL);

                if (smear != null)
                {
                    if (smear.bbslezy$smearArc() > 0F && SmearReplayState.has(context.entity))
                    {
                        bbslezy$renderFormArc(renderer, context, form, smear);
                        return;
                    }

                    if (smear.bbslezy$smearArc() <= 0F && smear.bbslezy$smearManual() > 0F)
                    {
                        bbslezy$renderFormVector(renderer, context, form, smear);
                        return;
                    }
                }
            }

            renderer.render(context);
            return;
        }

        Pose pose = model.getPose();

        Set<String> arcBones = new HashSet<>();
        Set<String> vectorBones = new HashSet<>();

        for (Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
        {
            ISmearBone s = (ISmearBone) entry.getValue();

            if (s.bbslezy$smearArc() > 0F)
            {
                arcBones.add(entry.getKey());
            }
            else if (s.bbslezy$smearManual() > 0F)
            {
                vectorBones.add(entry.getKey());
            }
        }

        boolean doArc = !arcBones.isEmpty() && SmearReplayState.has(context.entity);
        boolean doVector = !vectorBones.isEmpty();

        if (!doArc && !doVector)
        {
            renderer.render(context);
            return;
        }

        int original = context.color;
        Set<String> allBones = new HashSet<>(model.getBones());

        try
        {
            SmearRenderState.linesPending = true;
            context.color = original;
            renderer.render(context);

            if (doArc)
            {
                Set<String> hide = new HashSet<>(allBones);
                hide.removeAll(bbslezy$withDescendants(model, arcBones));
                bbslezy$arcCopies(renderer, model, context, form, pose, arcBones, hide, original);
            }

            if (doVector)
            {
                Set<String> hide = new HashSet<>(allBones);
                hide.removeAll(bbslezy$withDescendants(model, vectorBones));
                bbslezy$vectorCopies(renderer, model, context, pose, vectorBones, hide, original);
            }
        }
        finally
        {
            SmearRenderState.end();
            SmearRenderState.linesPending = false;
            context.color = original;
        }

        if (model instanceof IMotionLineDrawer drawer)
        {
            drawer.bbslezy$drawDeferredLines();
        }
    }

    @Unique
    private static void bbslezy$vectorCopies(FormRenderer renderer, ModelFormRenderer model, FormRenderingContext context, Pose pose, Set<String> vectorBones, Set<String> hide, int original)
    {
        ISmearBone lead = (ISmearBone) pose.transforms.get(vectorBones.iterator().next());
        int count = Math.max(1, Math.round(lead.bbslezy$smearCount() > 0F ? lead.bbslezy$smearCount() : 4F));
        float falloff = Math.max(lead.bbslezy$smearFalloff() > 0F ? lead.bbslezy$smearFalloff() : 0.6F, 0.001F);
        float dissolveAmount = lead.bbslezy$smearDissolve();

        for (int k = count; k >= 1; k--)
        {
            float fraction = (float) k / count;
            float alpha = (float) Math.pow(falloff, k);

            SmearRenderState.begin(model, -fraction, 1.5F, fraction * dissolveAmount);
            SmearRenderState.hide = hide;
            context.color = bbslezy$withAlpha(original, alpha);
            renderer.render(context);
            SmearRenderState.end();
        }
    }

    @Unique
    private static void bbslezy$arcCopies(FormRenderer renderer, ModelFormRenderer model, FormRenderingContext context, Form form, Pose pose, Set<String> arcBones, Set<String> hide, int original)
    {
        ISmearBone lead = (ISmearBone) pose.transforms.get(arcBones.iterator().next());
        int count = Math.max(1, Math.round(lead.bbslezy$smearCount() > 0F ? lead.bbslezy$smearCount() : 4F));
        float fade = Math.max(0F, Math.min(1F, lead.bbslezy$smearFalloff()));
        float dissolveAmount = lead.bbslezy$smearDissolve();
        float timeTicks = lead.bbslezy$smearTime() > 0F ? lead.bbslezy$smearTime() : 4F;
        float baseStretch = Math.max(0F, lead.bbslezy$smearStretch());

        Replay replay = SmearReplayState.replay;
        float now = SmearReplayState.time();

        if (!bbslezy$rewindMoves(form, replay, model, now, timeTicks))
        {
            replay.properties.applyProperties(form, now);
            return;
        }

        RenderSystem.depthMask(false);

        int density = Math.round(lead.bbslezy$smearDensity() > 0F ? lead.bbslezy$smearDensity() : 16F);
        float bandOpacity = lead.bbslezy$smearOpacity() > 0F ? lead.bbslezy$smearOpacity() : 0.14F;

        for (int j = density; j >= 1; j--)
        {
            float f = (float) j / density;
            float tk = now - f * timeTicks;
            float copyStretch = baseStretch * f;

            replay.properties.applyProperties(form, tk);

            Matrix4f root = bbslezy$rootBegin(context, replay, tk, now);
            SmearRenderState.beginArc(model, f * dissolveAmount, hide, copyStretch, f, pose);
            context.color = bbslezy$withAlpha(bbslezy$desaturate(original, 0.6F * f), bandOpacity);
            renderer.render(context);
            SmearRenderState.end();
            bbslezy$rootEnd(context, root);
        }

        RenderSystem.depthMask(true);

        for (int k = count; k >= 1; k--)
        {
            float f = (float) k / count;
            float tk = now - f * timeTicks;
            float alpha = 1F - fade * f;
            float copyStretch = baseStretch * f;

            replay.properties.applyProperties(form, tk);

            Matrix4f root = bbslezy$rootBegin(context, replay, tk, now);
            SmearRenderState.beginArc(model, f * dissolveAmount, hide, copyStretch, f, pose);
            context.color = bbslezy$withAlpha(bbslezy$desaturate(original, 0.6F * f), alpha);
            renderer.render(context);
            SmearRenderState.end();
            bbslezy$rootEnd(context, root);
        }

        replay.properties.applyProperties(form, now);
        context.color = original;
    }

    @Unique
    private static boolean bbslezy$rewindMoves(Form form, Replay replay, ModelFormRenderer model, float now, float timeTicks)
    {
        float far = now - timeTicks;

        if (Math.abs(replay.keyframes.x.interpolate(far).doubleValue() - replay.keyframes.x.interpolate(now).doubleValue()) > 1e-4
            || Math.abs(replay.keyframes.y.interpolate(far).doubleValue() - replay.keyframes.y.interpolate(now).doubleValue()) > 1e-4
            || Math.abs(replay.keyframes.z.interpolate(far).doubleValue() - replay.keyframes.z.interpolate(now).doubleValue()) > 1e-4)
        {
            return true;
        }

        Pose present = model.getPose();
        replay.properties.applyProperties(form, far);
        Pose rewound = model.getPose();

        return !rewound.transforms.equals(present.transforms);
    }

    @Unique
    private static void bbslezy$renderFormArc(FormRenderer renderer, FormRenderingContext context, Form form, ISmearBone smear)
    {
        int count = Math.max(1, Math.round(smear.bbslezy$smearCount() > 0F ? smear.bbslezy$smearCount() : 4F));
        float fade = Math.max(0F, Math.min(1F, smear.bbslezy$smearFalloff()));
        float dissolveAmount = smear.bbslezy$smearDissolve();
        float timeTicks = smear.bbslezy$smearTime() > 0F ? smear.bbslezy$smearTime() : 4F;
        int density = Math.round(smear.bbslezy$smearDensity() > 0F ? smear.bbslezy$smearDensity() : 16F);
        float bandOpacity = smear.bbslezy$smearOpacity() > 0F ? smear.bbslezy$smearOpacity() : 0.14F;
        int original = context.color;

        Replay replay = SmearReplayState.replay;
        float now = SmearReplayState.time();

        context.color = original;
        renderer.render(context);

        RenderSystem.depthMask(false);

        for (int j = density; j >= 1; j--)
        {
            float f = (float) j / density;
            float tk = now - f * timeTicks;

            replay.properties.applyProperties(form, tk);
            Matrix4f root = bbslezy$rootBegin(context, replay, tk, now);
            SmearRenderState.beginArc(renderer, f * dissolveAmount, null, 0F, f, null);
            context.color = bbslezy$withAlpha(bbslezy$desaturate(original, 0.6F * f), bandOpacity);
            renderer.render(context);
            SmearRenderState.end();
            bbslezy$rootEnd(context, root);
        }

        RenderSystem.depthMask(true);

        for (int k = count; k >= 1; k--)
        {
            float f = (float) k / count;
            float tk = now - f * timeTicks;
            float alpha = 1F - fade * f;

            replay.properties.applyProperties(form, tk);
            Matrix4f root = bbslezy$rootBegin(context, replay, tk, now);
            SmearRenderState.beginArc(renderer, f * dissolveAmount, null, 0F, f, null);
            context.color = bbslezy$withAlpha(bbslezy$desaturate(original, 0.6F * f), alpha);
            renderer.render(context);
            SmearRenderState.end();
            bbslezy$rootEnd(context, root);
        }

        replay.properties.applyProperties(form, now);
        context.color = original;
    }

    @Unique
    private static void bbslezy$renderFormVector(FormRenderer renderer, FormRenderingContext context, Form form, ISmearBone smear)
    {
        int count = Math.max(1, Math.round(smear.bbslezy$smearCount() > 0F ? smear.bbslezy$smearCount() : 4F));
        float falloff = Math.max(smear.bbslezy$smearFalloff() > 0F ? smear.bbslezy$smearFalloff() : 0.6F, 0.001F);
        float dissolveAmount = smear.bbslezy$smearDissolve();
        float sx = smear.bbslezy$smearX();
        float sy = smear.bbslezy$smearY();
        float sz = smear.bbslezy$smearZ();
        int original = context.color;

        Vector3f translate = form.transform.get().translate;
        float ox = translate.x;
        float oy = translate.y;
        float oz = translate.z;

        for (int k = count; k >= 1; k--)
        {
            float fraction = (float) k / count;
            float alpha = (float) Math.pow(falloff, k);

            translate.set(ox - sx * fraction, oy - sy * fraction, oz - sz * fraction);
            SmearRenderState.beginArc(renderer, fraction * dissolveAmount, null, 0F, fraction, null);
            context.color = bbslezy$withAlpha(original, alpha);
            renderer.render(context);
            SmearRenderState.end();
        }

        translate.set(ox, oy, oz);
        context.color = original;
        renderer.render(context);
    }

    @Unique
    private static Matrix4f bbslezy$rootBegin(FormRenderingContext context, Replay replay, float tk, float now)
    {
        double dx = replay.keyframes.x.interpolate(tk).doubleValue() - replay.keyframes.x.interpolate(now).doubleValue();
        double dy = replay.keyframes.y.interpolate(tk).doubleValue() - replay.keyframes.y.interpolate(now).doubleValue();
        double dz = replay.keyframes.z.interpolate(tk).doubleValue() - replay.keyframes.z.interpolate(now).doubleValue();

        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) < 1e-5)
        {
            return null;
        }

        Vector3f delta = new Vector3f((float) dx, (float) dy, (float) dz);
        Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();

        new Quaternionf(camRot).conjugate().transform(delta);

        Matrix4f m = context.stack.peek().getPositionMatrix();
        Matrix4f saved = new Matrix4f(m);

        m.set(new Matrix4f().translation(delta).mul(m));

        return saved;
    }

    @Unique
    private static void bbslezy$rootEnd(FormRenderingContext context, Matrix4f saved)
    {
        if (saved != null)
        {
            context.stack.peek().getPositionMatrix().set(saved);
        }
    }

    @Unique
    private static Set<String> bbslezy$withDescendants(ModelFormRenderer model, Set<String> bones)
    {
        Set<String> result = new HashSet<>(bones);

        if (model.getModel() == null || model.getModel().model == null)
        {
            return result;
        }

        Deque<String> stack = new ArrayDeque<>(bones);

        while (!stack.isEmpty())
        {
            Collection<String> children = model.getModel().model.getDirectChildrenKeys(stack.pop());

            if (children != null)
            {
                for (String child : children)
                {
                    if (result.add(child))
                    {
                        stack.push(child);
                    }
                }
            }
        }

        return result;
    }

    @Unique
    private static int bbslezy$withAlpha(int color, float alpha)
    {
        int a = Math.min(255, Math.max(0, Math.round((color >>> 24) * alpha)));

        return (color & 0x00FFFFFF) | (a << 24);
    }

    @Unique
    private static int bbslezy$desaturate(int color, float amount)
    {
        if (amount <= 0F)
        {
            return color;
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int grey = Math.round(0.299F * r + 0.587F * g + 0.114F * b);

        r = Math.round(r + (grey - r) * amount);
        g = Math.round(g + (grey - g) * amount);
        b = Math.round(b + (grey - b) * amount);

        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
