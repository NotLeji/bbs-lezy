package leji.bbslezy.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import leji.bbslezy.vfx.forms.CurveForm;
import leji.bbslezy.vfx.forms.CurvePoint;
import leji.bbslezy.vfx.forms.CurveSpline;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link CurveForm} in the world: the Catmull-Rom spline as a coloured line, or — when the
 * form's {@code extrude} toggle is on — a solid tube swept along the spline (radius = width / 2).
 */
public class CurveFormRenderer extends FormRenderer<CurveForm>
{
    private static final int TUBE_SIDES = 16;
    private static final float[] RING_COS = new float[TUBE_SIDES];
    private static final float[] RING_SIN = new float[TUBE_SIDES];

    static
    {
        for (int i = 0; i < TUBE_SIDES; i++)
        {
            float a = (float) (2 * Math.PI * i / TUBE_SIDES);

            RING_COS[i] = (float) Math.cos(a);
            RING_SIN[i] = (float) Math.sin(a);
        }
    }

    public CurveFormRenderer(CurveForm form)
    {
        super(form);
    }

    @Override
    public List<String> getBones()
    {
        return this.form.boneNames();
    }

    @Override
    public void collectMatrices(IEntity entity, MatrixStack stack, MatrixCache matrices, String prefix, float transition)
    {
        super.collectMatrices(entity, stack, matrices, prefix, transition);

        List<CurvePoint> points = this.form.points.getAllTyped();

        if (points.isEmpty())
        {
            return;
        }

        stack.push();
        this.applyTransforms(stack, false, transition);

        for (CurvePoint point : points)
        {
            Vector3f pos = this.form.pointPosition(point);
            Vector3f rot = this.form.pointRotation(point);

            stack.push();
            stack.translate(pos.x, pos.y, pos.z);

            Matrix4f origin = new Matrix4f(stack.peek().getPositionMatrix());

            stack.multiply(new Quaternionf().rotationZYX(
                (float) Math.toRadians(rot.z), (float) Math.toRadians(rot.y), (float) Math.toRadians(rot.x)));

            Matrix4f matrix = new Matrix4f(stack.peek().getPositionMatrix());

            stack.pop();

            matrices.put(StringUtils.combinePaths(prefix, point.bone.get()), matrix, origin);
        }

        stack.pop();
    }

    public Matrix4f formTransformMatrix(float transition)
    {
        Matrix4f matrix = new Matrix4f();

        this.applyTransforms(matrix, transition);

        return matrix;
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        CurveFormThumbnailRenderer.render(context, this.form, x1, y1, x2 - x1, y2 - y1);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        if (BBSRendering.isIrisShadowPass() || context.stencilMap != null)
        {
            return;
        }

        List<CurveSpline.Sample> samples = this.form.sampleRendered();

        if (samples.size() < 2)
        {
            return;
        }

        Matrix4f mat = context.stack.peek().getPositionMatrix();

        boolean solid = this.form.extrudeSolid.get();
        boolean blocks = this.form.extrudeBlocks.get() && this.form.extrudeBlock.get() != null;

        if (!solid && !blocks)
        {
            this.renderLine(mat, samples);
        }

        if (solid)
        {
            this.renderTube(mat, samples);
        }

        if (blocks)
        {
            this.renderBlocks(context, samples);
        }
    }

    private void renderBlocks(FormRenderingContext context, List<CurveSpline.Sample> path)
    {
        BlockState state = this.form.extrudeBlock.get();
        BlockForm block = new BlockForm();

        block.blockState.set(state);

        float spacing = Math.max(0.05F, this.form.extrudeSpacing.get());
        float scale = this.form.extrudeScale.get();
        boolean align = this.form.extrudeAlign.get();
        boolean connect = this.form.extrudeConnect.get() && align;

        int last = path.size() - 1;

        if (last < 1)
        {
            return;
        }

        List<Vector3f> pts = new ArrayList<>();
        List<Float> ts = new ArrayList<>();

        pts.add(new Vector3f(path.get(0).pos));
        ts.add(0F);

        float accumulated = 0F;

        for (int i = 0; i < last; i++)
        {
            Vector3f p0 = path.get(i).pos;
            Vector3f p1 = path.get(i + 1).pos;
            float segLen = new Vector3f(p1).sub(p0).length();

            if (segLen < 1e-6F)
            {
                continue;
            }

            accumulated += segLen;

            while (accumulated >= spacing)
            {
                accumulated -= spacing;

                float localT = 1F - accumulated / segLen;
                pts.add(new Vector3f(p0).lerp(p1, localT));
                ts.add((i + localT) / last);
            }
        }

        if (connect && pts.size() >= 2)
        {
            for (int j = 0; j < pts.size() - 1; j++)
            {
                Vector3f a = pts.get(j);
                Vector3f b = pts.get(j + 1);
                Vector3f dir = new Vector3f(b).sub(a);
                float len = dir.length();

                if (len < 1e-6F)
                {
                    continue;
                }

                Vector3f mid = new Vector3f(a).add(b).mul(0.5F);
                Vector3f rot = this.form.rotationAt((ts.get(j) + ts.get(j + 1)) * 0.5F);

                placeBlockSpan(context, block, mid, dir, rot, scale, len);
            }
        }
        else
        {
            for (int j = 0; j < pts.size(); j++)
            {
                Vector3f dir = j < pts.size() - 1
                    ? new Vector3f(pts.get(j + 1)).sub(pts.get(j))
                    : new Vector3f(pts.get(j)).sub(pts.get(Math.max(0, j - 1)));

                placeBlock(context, block, pts.get(j), dir, this.form.rotationAt(ts.get(j)), scale, align);
            }
        }
    }

    private static void placeBlock(FormRenderingContext context, BlockForm block, Vector3f pos, Vector3f dir, Vector3f rotDeg, float scale, boolean align)
    {
        MatrixStack stack = context.stack;

        stack.push();
        stack.translate(pos.x, pos.y, pos.z);

        applyAlignAndRotation(stack, dir, rotDeg, align);

        if (scale != 1F)
        {
            stack.scale(scale, scale, scale);
        }

        FormUtilsClient.render(block, context);
        stack.pop();
    }

    private static void placeBlockSpan(FormRenderingContext context, BlockForm block, Vector3f mid, Vector3f dir, Vector3f rotDeg, float crossScale, float len)
    {
        MatrixStack stack = context.stack;

        stack.push();
        stack.translate(mid.x, mid.y, mid.z);

        applyAlignAndRotation(stack, dir, rotDeg, true);

        stack.scale(crossScale, crossScale, len);

        FormUtilsClient.render(block, context);
        stack.pop();
    }

    private static void applyAlignAndRotation(MatrixStack stack, Vector3f dir, Vector3f rotDeg, boolean align)
    {
        if (align && dir.lengthSquared() > 1e-12F)
        {
            Vector3f d = new Vector3f(dir).normalize();
            float yaw = (float) Math.atan2(d.x, d.z);
            float pitch = (float) Math.asin(Math.max(-1F, Math.min(1F, -d.y)));

            stack.multiply(new Quaternionf().rotateY(yaw).rotateX(pitch));
        }

        if (rotDeg.x != 0F || rotDeg.y != 0F || rotDeg.z != 0F)
        {
            stack.multiply(new Quaternionf().rotateXYZ(
                (float) Math.toRadians(rotDeg.x),
                (float) Math.toRadians(rotDeg.y),
                (float) Math.toRadians(rotDeg.z)));
        }
    }

    private void renderLine(Matrix4f mat, List<CurveSpline.Sample> path)
    {
        Color c = this.form.color.get();

        setupState();

        BufferBuilder builder = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (CurveSpline.Sample s : path)
        {
            LezyVfxRenderCompat.next(builder.vertex(mat, s.pos.x, s.pos.y, s.pos.z).color(c.r, c.g, c.b, c.a));
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        restoreState();
    }

    private void renderTube(Matrix4f mat, List<CurveSpline.Sample> path)
    {
        int n = path.size();
        Color c = this.form.color.get();
        float r = c.r, g = c.g, b = c.b, a = c.a;
        float widthMul = Math.max(0.001F, this.form.width.get());

        Vector3f[] pos = new Vector3f[n];
        float[] rad = new float[n];

        for (int i = 0; i < n; i++)
        {
            pos[i] = path.get(i).pos;
            rad[i] = Math.max(0.0005F, path.get(i).width * widthMul * 0.5F);
        }

        Vector3f[] tan = new Vector3f[n];

        for (int i = 0; i < n; i++)
        {
            Vector3f t;

            if (i == 0)
            {
                t = new Vector3f(pos[1]).sub(pos[0]);
            }
            else if (i == n - 1)
            {
                t = new Vector3f(pos[i]).sub(pos[i - 1]);
            }
            else
            {
                t = new Vector3f(pos[i + 1]).sub(pos[i - 1]);
            }

            if (t.lengthSquared() < 1e-12F)
            {
                t.set(0, 0, 1);
            }
            else
            {
                t.normalize();
            }

            tan[i] = t;
        }

        Vector3f[] nrm = new Vector3f[n];
        Vector3f seed = new Vector3f(tan[0]).cross(0, 1, 0);

        if (seed.lengthSquared() < 1e-6F)
        {
            seed = new Vector3f(tan[0]).cross(1, 0, 0);
        }

        nrm[0] = seed.normalize();

        for (int i = 1; i < n; i++)
        {
            Vector3f axis = new Vector3f(tan[i - 1]).cross(tan[i]);
            Vector3f next;

            if (axis.lengthSquared() < 1e-9F)
            {
                next = new Vector3f(nrm[i - 1]);
            }
            else
            {
                axis.normalize();

                float dot = Math.max(-1F, Math.min(1F, tan[i - 1].dot(tan[i])));
                float angle = (float) Math.acos(dot);

                next = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle).transform(new Vector3f(nrm[i - 1]));
            }

            next.sub(new Vector3f(tan[i]).mul(next.dot(tan[i])));

            if (next.lengthSquared() < 1e-9F)
            {
                next = new Vector3f(nrm[i - 1]);
            }

            nrm[i] = next.normalize();
        }

        setupState();

        BufferBuilder builder = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Vector3f[] prev = ring(pos[0], tan[0], nrm[0], rad[0]);

        for (int i = 1; i < n; i++)
        {
            Vector3f[] cur = ring(pos[i], tan[i], nrm[i], rad[i]);

            for (int s = 0; s < TUBE_SIDES; s++)
            {
                int s2 = (s + 1) % TUBE_SIDES;
                Vector3f a0 = prev[s], a1 = prev[s2], b0 = cur[s], b1 = cur[s2];

                vertex(builder, mat, a0, r, g, b, a);
                vertex(builder, mat, a1, r, g, b, a);
                vertex(builder, mat, b1, r, g, b, a);

                vertex(builder, mat, a0, r, g, b, a);
                vertex(builder, mat, b1, r, g, b, a);
                vertex(builder, mat, b0, r, g, b, a);
            }

            prev = cur;
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        restoreState();
    }

    private static Vector3f[] ring(Vector3f center, Vector3f tangent, Vector3f normal, float radius)
    {
        Vector3f binormal = new Vector3f(tangent).cross(normal).normalize();
        Vector3f[] out = new Vector3f[TUBE_SIDES];

        for (int s = 0; s < TUBE_SIDES; s++)
        {
            Vector3f off = new Vector3f(normal).mul(RING_COS[s] * radius)
                .add(new Vector3f(binormal).mul(RING_SIN[s] * radius));

            out[s] = new Vector3f(center).add(off);
        }

        return out;
    }

    private static void vertex(BufferBuilder builder, Matrix4f mat, Vector3f v, float r, float g, float b, float a)
    {
        LezyVfxRenderCompat.next(builder.vertex(mat, v.x, v.y, v.z).color(r, g, b, a));
    }

    private static void setupState()
    {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        /* Honour the active whole-form blend (set per top-level form by FormBlendStateMixin); plain
         * alpha otherwise. */
        if (!(BbsVfxBlendState.active && BbsVfxBlendGL.apply(BbsVfxBlendState.mode, BbsVfxBlendState.factor)))
        {
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.disableCull();
    }

    private static void restoreState()
    {
        /* Reset the equation in case a Darken/Lighten MIN/MAX was set; harmless when unchanged. */
        BbsVfxBlendGL.resetEquation();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
