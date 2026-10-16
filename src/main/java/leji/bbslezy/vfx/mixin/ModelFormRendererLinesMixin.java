package leji.bbslezy.vfx.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import leji.bbslezy.vfx.client.BbsVfxArcTrail;
import leji.bbslezy.vfx.client.BbsVfxTextureColor;
import leji.bbslezy.vfx.client.IArcTrailDrawer;
import leji.bbslezy.vfx.client.IMotionLineDrawer;
import leji.bbslezy.vfx.client.LezyVfxRenderCompat;
import leji.bbslezy.vfx.client.SmearRenderState;
import leji.bbslezy.vfx.client.SmearReplayState;
import leji.bbslezy.vfx.forms.IModelSmearChannels;
import leji.bbslezy.vfx.forms.ISmearBone;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.IAnimator;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws smear motion lines: streaks trailing each bone of the motion_lines channel.
 */
@Mixin(value = ModelFormRenderer.class, remap = false)
public abstract class ModelFormRendererLinesMixin implements IArcTrailDrawer, IMotionLineDrawer
{
    @Shadow
    private MatrixCache bones;

    @Shadow
    public abstract Form getForm();

    @Shadow
    public abstract ModelInstance getModel();

    @Shadow
    private IAnimator animator;

    @Shadow
    public abstract Pose getPose();

    @Shadow
    private void captureMatrices(ModelInstance model)
    {
        throw new AssertionError();
    }

    @Unique
    private static boolean bbslezy$trailLogged;

    @Unique
    private Map<String, Vector3f> bbslezy$offsets;

    @Unique
    private Matrix4f bbslezy$lineMat;

    @Unique
    private Link bbslezy$textureLink()
    {
        Link link = this.getForm() instanceof ModelForm form ? form.texture.get() : null;

        if (link == null)
        {
            ModelInstance model = this.getModel();

            if (model != null)
            {
                link = model.getTexture();
            }
        }

        return link;
    }

    @Unique
    private Pose bbslezy$linesPose()
    {
        return this.getForm() instanceof IModelSmearChannels channels ? channels.bbslezy$linesPose().get() : null;
    }

    @Inject(method = "render3D", at = @At("TAIL"))
    private void bbslezy$motionLines(FormRenderingContext context, CallbackInfo ci)
    {
        if (context.stencilMap != null)
        {
            return;
        }

        if (SmearRenderState.active && SmearRenderState.arc && BbsVfxArcTrail.active)
        {
            Set<String> hidden = SmearRenderState.hide;

            for (Map.Entry<String, MatrixCacheEntry> entry : this.bones.entrySet())
            {
                if (hidden != null && hidden.contains(entry.getKey()))
                {
                    continue;
                }

                MatrixCacheEntry self = entry.getValue();

                if (self != null && self.matrix() != null)
                {
                    BbsVfxArcTrail.setPivot(entry.getKey(), self.matrix().getTranslation(new Vector3f()));
                }

                String tip = this.bbslezy$tipBone(entry.getKey());
                MatrixCacheEntry tipEntry = tip.equals(entry.getKey()) ? self : this.bones.get(tip);

                if (tipEntry != null && tipEntry.matrix() != null)
                {
                    BbsVfxArcTrail.add(entry.getKey(), tipEntry.matrix().getTranslation(new Vector3f()));
                }
            }
        }
    }

    @Inject(method = "renderBodyParts", at = @At("TAIL"))
    private void bbslezy$arcLines(FormRenderingContext context, CallbackInfo ci)
    {
        if (context.stencilMap != null || SmearRenderState.active)
        {
            return;
        }

        if (this.bbslezy$linesPose() == null || !SmearReplayState.has(context.entity))
        {
            return;
        }

        Matrix4f mat = new Matrix4f(context.stack.peek().getPositionMatrix());

        if (SmearRenderState.linesPending)
        {
            this.bbslezy$lineMat = mat;
            return;
        }

        this.bbslezy$drawMotionLines(mat, context.entity, context.getTransition());
    }

    @Override
    public void bbslezy$drawDeferredLines()
    {
        if (this.bbslezy$lineMat == null)
        {
            return;
        }

        Matrix4f mat = this.bbslezy$lineMat;
        this.bbslezy$lineMat = null;

        if (this.bbslezy$linesPose() == null || SmearReplayState.replay == null)
        {
            return;
        }

        this.bbslezy$drawMotionLines(mat, SmearReplayState.entity, SmearReplayState.transition);
    }

    @Unique
    private void bbslezy$drawMotionLines(Matrix4f mat, IEntity entity, float transition)
    {
        Pose lines = this.bbslezy$linesPose();

        if (lines == null)
        {
            return;
        }

        Replay replay = SmearReplayState.replay;

        if (replay == null)
        {
            return;
        }

        float now = SmearReplayState.time();
        Form form = this.getForm();

        ShaderProgram previous = RenderSystem.getShader();
        boolean drew = false;

        for (Map.Entry<String, PoseTransform> entry : lines.transforms.entrySet())
        {
            ISmearBone line = (ISmearBone) entry.getValue();

            if (line.bbslezy$smearLines() <= 0F)
            {
                continue;
            }

            if (line.bbslezy$smearLines() >= 1.5F)
            {
                Vector3f pos = this.bbslezy$boneAtNow(entity, transition, replay, form, now, entry.getKey());

                if (pos != null)
                {
                    this.bbslezy$drawManualLines(mat, pos, entry.getValue(), line);
                    drew = true;
                }

                continue;
            }

            int count = line.bbslezy$smearLinesCount() > 0F ? Math.max(1, Math.round(line.bbslezy$smearLinesCount())) : 7;
            Vector3f[][] arcs = this.bbslezy$sampleLimb(entity, transition, replay, form, now, entry.getKey(), count, line);

            if (arcs != null)
            {
                this.bbslezy$drawLimbLines(mat, arcs, entry.getValue(), line);
                drew = true;
            }
        }

        replay.properties.applyProperties(form, now);
        ModelInstance model = this.getModel();

        if (model != null && this.animator != null)
        {
            model.model.resetPose();
            this.animator.applyActions(entity, model, transition);
            model.model.applyPose(this.getPose());
            model.form = this.getForm();
            mchorse.bbs_mod.cubic.ik.ModelIKRuntime.apply(model, null, null);
        }

        this.bones.clear();

        if (drew)
        {
            RenderSystem.setShader(() -> previous);
        }
    }

    @Unique
    private Vector3f bbslezy$boneAtNow(IEntity entity, float transition, Replay replay, Form form, float now, String key)
    {
        ModelInstance model = this.getModel();

        if (model == null || this.animator == null || model.model == null)
        {
            return null;
        }

        replay.properties.applyProperties(form, now);
        model.model.resetPose();
        this.animator.applyActions(entity, model, transition);
        model.model.applyPose(this.getPose());
        model.form = this.getForm();
        mchorse.bbs_mod.cubic.ik.ModelIKRuntime.apply(model, null, null);
        this.captureMatrices(model);

        MatrixCacheEntry e = this.bones.get(this.bbslezy$tipBone(key));
        Vector3f pos = e != null && e.matrix() != null ? e.matrix().getTranslation(new Vector3f()) : null;

        this.bones.clear();

        return pos;
    }

    @Unique
    private Vector3f[][] bbslezy$sampleLimb(IEntity entity, float transition, Replay replay, Form form, float now, String key, int count, ISmearBone line)
    {
        ModelInstance model = this.getModel();

        if (model == null || this.animator == null || model.model == null)
        {
            return null;
        }

        String end = this.bbslezy$tipBone(key);
        String base = model.model.getParentGroupKey(key);

        if (base == null)
        {
            base = key;
        }

        int samples = 10;
        float timeTicks = line.bbslezy$smearTime() > 0F ? line.bbslezy$smearTime() : 4F;
        float window = Math.min(timeTicks, now);

        if (window < 0.05F)
        {
            return null;
        }

        float reach = line.bbslezy$smearLinesOffsetX();
        Vector3f[] tip = new Vector3f[samples];
        Vector3f baseNow = null;

        for (int j = 0; j < samples; j++)
        {
            float f = (float) j / (samples - 1);
            float tk = now - f * window;

            replay.properties.applyProperties(form, tk);
            model.model.resetPose();
            this.animator.applyActions(entity, model, transition);
            model.model.applyPose(this.getPose());
            model.form = this.getForm();
            mchorse.bbs_mod.cubic.ik.ModelIKRuntime.apply(model, null, null);
            this.captureMatrices(model);

            MatrixCacheEntry be = this.bones.get(base);
            MatrixCacheEntry ee = this.bones.get(end);
            Vector3f bp = be != null && be.matrix() != null ? be.matrix().getTranslation(new Vector3f()) : null;
            Vector3f ep = ee != null && ee.matrix() != null ? ee.matrix().getTranslation(new Vector3f()) : null;

            this.bones.clear();

            if (ep == null)
            {
                tip[j] = j > 0 ? tip[j - 1] : null;
                continue;
            }

            Vector3f endAnchor = new Vector3f(ep);

            if (bp != null)
            {
                Vector3f limb = new Vector3f(ep).sub(bp);
                float len = limb.length();

                if (len > 1e-4F)
                {
                    float ext = reach > 0F ? reach : Math.min(len * 0.6F, 0.3F);
                    endAnchor.add(new Vector3f(limb).div(len).mul(ext));
                }
            }

            tip[j] = endAnchor;

            if (j == 0)
            {
                baseNow = bp != null ? bp : new Vector3f(ep);
            }
        }

        if (tip[0] == null)
        {
            return null;
        }

        return new Vector3f[][] { tip, { baseNow != null ? baseNow : tip[0] } };
    }

    @Unique
    private void bbslezy$drawLimbLines(Matrix4f mat, Vector3f[][] data, PoseTransform transform, ISmearBone line)
    {
        Vector3f[] tip = data[0];
        Vector3f baseNow = data[1][0];
        int n = tip.length;

        if (tip[0] == null || tip[n - 1] == null)
        {
            return;
        }

        Vector3f head = tip[0];
        float span = head.distance(tip[n - 1]);
        float recent = head.distance(tip[Math.min(3, n - 1)]);

        if (span < 1e-4F || recent < 5e-3F)
        {
            return;
        }

        int count = line.bbslezy$smearLinesCount() > 0F ? Math.max(1, Math.round(line.bbslezy$smearLinesCount())) : 9;
        float thickness = line.bbslezy$smearLinesWidth() > 0F ? line.bbslezy$smearLinesWidth() : 0.035F;
        float volume = line.bbslezy$smearLinesSpread() > 0F ? line.bbslezy$smearLinesSpread() : 0.14F;
        float lengthMul = line.bbslezy$smearLinesOffsetY() > 0F ? line.bbslezy$smearLinesOffsetY() : 1F;
        boolean blunt = line.bbslezy$smearLinesOffsetZ() > 0F;

        float cr = transform.color.r;
        float cg = transform.color.g;
        float cb = transform.color.b;
        float ca = transform.color.a;

        if (line.bbslezy$smearLinesTexture() > 0F)
        {
            int argb = BbsVfxTextureColor.average(this.bbslezy$textureLink());
            cr = ((argb >> 16) & 0xFF) / 255F;
            cg = ((argb >> 8) & 0xFF) / 255F;
            cb = (argb & 0xFF) / 255F;
        }

        Vector3f limbDir = new Vector3f(head).sub(baseNow);
        limbDir = limbDir.lengthSquared() < 1e-8F ? new Vector3f(0F, 1F, 0F) : limbDir.normalize();
        Vector3f pu = new Vector3f(limbDir).cross(0F, 1F, 0F);
        pu = pu.lengthSquared() < 1e-6F ? new Vector3f(1F, 0F, 0F) : pu.normalize();
        Vector3f pv = new Vector3f(limbDir).cross(pu).normalize();

        Vector3f cam = new Matrix4f(mat).invert().transformPosition(new Vector3f());

        BufferBuilder builder;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Vector3f[] p = new Vector3f[n];

        for (int s = 0; s < count; s++)
        {
            float u = 0.05F + 0.95F * bbslezy$hash(s, 17);
            double ang = bbslezy$hash(s, 3) * Math.PI * 2.0;
            float r = volume * (0.2F + 0.8F * bbslezy$hash(s, 5));
            float ox = (float) (r * Math.cos(ang));
            float oy = (float) (r * Math.sin(ang));
            Vector3f origin = new Vector3f(baseNow).lerp(head, u).add(pu.x * ox + pv.x * oy, pu.y * ox + pv.y * oy, pu.z * ox + pv.z * oy);

            float lenJitter = 0.4F + 0.7F * bbslezy$hash(s, 7);
            float target = Math.min(recent * 4F * lenJitter, 0.7F) * lengthMul;
            float scale = target / span;

            for (int i = 0; i < n; i++)
            {
                p[i] = new Vector3f(tip[i]).sub(head).mul(scale).add(origin);
            }

            this.bbslezy$ribbonStrip(builder, mat, cam, p, thickness, blunt, cr, cg, cb, ca);
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    @Unique
    private void bbslezy$ribbonStrip(BufferBuilder builder, Matrix4f mat, Vector3f cam, Vector3f[] p, float thickness, boolean blunt, float cr, float cg, float cb, float ca)
    {
        int n = p.length;
        Vector3f[] w = new Vector3f[n];

        for (int i = 0; i < n; i++)
        {
            Vector3f tan = new Vector3f();

            if (i > 0)
            {
                Vector3f in = new Vector3f(p[i]).sub(p[i - 1]);

                if (in.lengthSquared() > 1e-12F)
                {
                    tan.add(in.normalize());
                }
            }

            if (i < n - 1)
            {
                Vector3f out = new Vector3f(p[i + 1]).sub(p[i]);

                if (out.lengthSquared() > 1e-12F)
                {
                    tan.add(out.normalize());
                }
            }

            Vector3f wi = tan.cross(new Vector3f(p[i]).sub(cam));
            w[i] = wi.lengthSquared() < 1e-12F ? new Vector3f(1F, 0F, 0F) : wi.normalize();
        }

        for (int i = 0; i < n - 1; i++)
        {
            float t0 = (float) i / (n - 1);
            float t1 = (float) (i + 1) / (n - 1);

            float h0 = blunt ? thickness * 0.5F : thickness * 0.5F * (1F - t0);
            float h1 = blunt ? thickness * 0.5F : thickness * 0.5F * (1F - t1);
            float a0 = ca * (1F - t0 * 0.85F);
            float a1 = ca * (1F - t1 * 0.85F);

            bbslezy$vert(builder, mat, p[i], w[i], h0, cr, cg, cb, a0, 1);
            bbslezy$vert(builder, mat, p[i], w[i], h0, cr, cg, cb, a0, -1);
            bbslezy$vert(builder, mat, p[i + 1], w[i + 1], h1, cr, cg, cb, a1, -1);
            bbslezy$vert(builder, mat, p[i + 1], w[i + 1], h1, cr, cg, cb, a1, 1);
        }
    }

    @Unique
    private static void bbslezy$vert(BufferBuilder builder, Matrix4f mat, Vector3f point, Vector3f w, float half, float r, float g, float b, float a, int sign)
    {
        LezyVfxRenderCompat.next(builder.vertex(mat, point.x + w.x * half * sign, point.y + w.y * half * sign, point.z + w.z * half * sign).color(r, g, b, a));
    }

    @Unique
    private void bbslezy$drawManualLines(Matrix4f mat, Vector3f pos, PoseTransform transform, ISmearBone line)
    {
        MatrixStack ms = new MatrixStack();
        ms.peek().getPositionMatrix().mul(mat);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.bbslezy$drawLines(ms, pos, new Vector3f(), transform, line);
    }

    @Unique
    private void bbslezy$drawLines(MatrixStack stack, Vector3f pos, Vector3f delta, PoseTransform transform, ISmearBone line)
    {
        float motion = delta.length();
        boolean hasMotion = motion >= 0.02F;

        Vector3f dir = hasMotion ? new Vector3f(delta).div(motion) : new Vector3f(1F, 0F, 0F);
        float length = hasMotion ? motion : (line.bbslezy$smearLinesOffsetY() > 0F ? line.bbslezy$smearLinesOffsetY() : 0.3F);

        Quaternionf align = new Quaternionf().rotationTo(new Vector3f(1F, 0F, 0F), dir);

        int count = line.bbslezy$smearLinesCount() > 0F ? Math.max(1, Math.round(line.bbslezy$smearLinesCount())) : 4;
        float thickness = line.bbslezy$smearLinesWidth() > 0F ? line.bbslezy$smearLinesWidth() : 0.02F;
        float spread = line.bbslezy$smearLinesSpread() > 0F ? line.bbslezy$smearLinesSpread() : Math.max(length * 0.08F, 0.02F);

        Color color = transform.color;
        float cr = color.r;
        float cg = color.g;
        float cb = color.b;
        float ca = color.a;

        if (line.bbslezy$smearLinesTexture() > 0F)
        {
            int argb = BbsVfxTextureColor.average(this.bbslezy$textureLink());

            cr = ((argb >> 16) & 0xFF) / 255F;
            cg = ((argb >> 8) & 0xFF) / 255F;
            cb = (argb & 0xFF) / 255F;
        }

        Vector3f off = transform.translate;
        Vector3f rot = transform.rotate;
        Vector3f scale = transform.scale;

        stack.push();
        stack.translate(pos.x + off.x, pos.y + off.y, pos.z + off.z);
        stack.multiply(new Quaternionf().rotationZYX(rot.z, rot.y, rot.x));
        stack.scale(scale.x, scale.y, scale.z);

        for (int i = 0; i < count; i++)
        {
            float oy = (bbslezy$hash(i, 1) * 2F - 1F) * spread;
            float oz = (bbslezy$hash(i, 2) * 2F - 1F) * spread;
            float len = length * (0.6F + 0.4F * bbslezy$hash(i, 3));

            stack.push();
            stack.multiply(align);
            stack.translate(0F, oy, oz);
            Draw.renderBox(stack, 0F, -thickness, -thickness, len, thickness * 2F, thickness * 2F, cr, cg, cb, ca);
            stack.pop();
        }

        stack.pop();
    }

    @Override
    public void bbslezy$drawArcTrail(FormRenderingContext context)
    {
        if (BbsVfxArcTrail.points.isEmpty())
        {
            return;
        }

        int argb = BbsVfxTextureColor.average(this.bbslezy$textureLink());
        float cr = Math.min(1F, ((argb >> 16) & 0xFF) / 255F + 0.45F);
        float cg = Math.min(1F, ((argb >> 8) & 0xFF) / 255F + 0.45F);
        float cb = Math.min(1F, (argb & 0xFF) / 255F + 0.45F);
        float ca = 0.4F;

        if (!bbslezy$trailLogged && !BbsVfxArcTrail.points.isEmpty())
        {
            List<Vector3f> first = BbsVfxArcTrail.points.values().iterator().next();

            if (first.size() >= 2)
            {
                float span = first.get(0).distance(first.get(first.size() - 1));

                if (span > 0.05F)
                {
                    bbslezy$trailLogged = true;
                    org.apache.logging.log4j.LogManager.getLogger("bbslezy").info("[smear-arc] trail: bones={} pts={} span={}",
                        BbsVfxArcTrail.points.size(), first.size(), span);
                }
            }
        }

        boolean any = false;

        for (List<Vector3f> pts : BbsVfxArcTrail.points.values())
        {
            if (pts.size() >= 2)
            {
                any = true;
                break;
            }
        }

        if (!any)
        {
            return;
        }

        ShaderProgram previous = RenderSystem.getShader();
        BufferBuilder builder;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Map.Entry<String, List<Vector3f>> entry : BbsVfxArcTrail.points.entrySet())
        {
            List<Vector3f> pts = entry.getValue();
            Vector3f pivot = BbsVfxArcTrail.pivots.get(entry.getKey());

            if (pts.size() < 2 || pivot == null)
            {
                continue;
            }

            for (int i = 0; i < pts.size() - 1; i++)
            {
                Vector3f a = pts.get(i);
                Vector3f b = pts.get(i + 1);

                Draw.fillQuad(builder, context.stack,
                    pivot.x, pivot.y, pivot.z,
                    a.x, a.y, a.z,
                    b.x, b.y, b.z,
                    b.x, b.y, b.z,
                    cr, cg, cb, ca);
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();

        if (previous != null)
        {
            RenderSystem.setShader(() -> previous);
        }
    }

    @Unique
    private String bbslezy$tipBone(String bone)
    {
        if (this.getModel() == null || this.getModel().model == null)
        {
            return bone;
        }

        String current = bone;

        for (int depth = 0; depth < 8; depth++)
        {
            java.util.Collection<String> children = this.getModel().model.getDirectChildrenKeys(current);

            if (children == null || children.isEmpty())
            {
                break;
            }

            current = children.iterator().next();
        }

        return current;
    }

    @Unique
    private static float bbslezy$hash(int i, int salt)
    {
        int h = (i * 73856093) ^ (salt * 19349663);

        h ^= h >>> 13;
        h *= 0x85ebca6b;
        h ^= h >>> 16;

        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }
}
