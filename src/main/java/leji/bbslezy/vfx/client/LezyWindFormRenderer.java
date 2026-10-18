package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import leji.bbslezy.vfx.forms.LezyWindForm;

/**
 * Renders a {@link LezyWindForm}'s visible ambient wind as a screen-space VOLUME ({@link LezyWindVolume})
 */
public class LezyWindFormRenderer extends FormRenderer<LezyWindForm>
{
    private static final float HEIGHT = 9F;

    private double lastFlow = Double.NaN;
    private double lastLeafFlow = Double.NaN;
    private float fixX, fixY, fixZ;

    public LezyWindFormRenderer(LezyWindForm form)
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        int y = (y1 + y2) / 2;

        context.batcher.box(x1 + 4, y - 6, x2 - 8, y - 4, 0x80FFFFFF);
        context.batcher.box(x1 + 10, y, x2 - 4, y + 2, 0xB0FFFFFF);
        context.batcher.box(x1 + 6, y + 6, x2 - 10, y + 8, 0x60FFFFFF);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        if (!this.form.scanned.get())
        {
            return;
        }

        boolean shadow = BBSRendering.isIrisShadowPass() || LezyVfxForeignPass.isActive();

        if (!shadow && context.type != FormRenderType.MODEL_BLOCK && context.type != FormRenderType.ENTITY)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null)
        {
            return;
        }

        double tFlow = (mc.world.getTime() + mc.getRenderTickCounter().getTickDelta(false)) / 20.0;

        Vector3f dir = this.form.point();
        float len = (float) Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float wu = len > 1.0E-4F ? dir.x / len : 1F;
        float wv = len > 1.0E-4F ? dir.z / len : 0F;

        float gust = 1F;
        double flowTime = tFlow;

        if (this.form.isStorm())
        {
            double t = tFlow;
            final double a1 = 0.35, w1 = 0.21;
            final double a2 = 0.20, w2 = 0.47, p2 = 1.7;

            gust = (float) (1.0 + a1 * Math.sin(w1 * t) + a2 * Math.sin(w2 * t + p2));
            flowTime = t - (a1 / w1) * Math.cos(w1 * t) - (a2 / w2) * Math.cos(w2 * t + p2);

            float wob = (float) Math.toRadians(7.0 * Math.sin(t * 0.13 + 2.1));
            float cs = (float) Math.cos(wob), sn = (float) Math.sin(wob);
            float nu = wu * cs - wv * sn;

            wv = wu * sn + wv * cs;
            wu = nu;
        }

        if (!shadow && this.form.opacity.get() > 0.001F && tFlow != this.lastFlow
            && (this.form.streaks.get() > 0.001F || this.form.dust.get() > 0.001F))
        {
            this.lastFlow = tFlow;

            mchorse.bbs_mod.utils.colors.Color col = this.form.color.get();
            boolean vortex = this.form.isVortex();

            float height = vortex ? this.form.funnelHeight.get() : HEIGHT;
            float speed = vortex
                ? Math.max(0.5F, this.form.swirl.get())
                : Math.max(0.5F, this.form.strength.get());

            float densCoef = vortex ? 0.13F : 0.30F;
            float dustCoef = vortex ? 0.12F : 0.30F;

            LezyWindVolume.queue(new Matrix4f(context.stack.peek().getPositionMatrix()),
                this.form.scanRadius.get(), height,
                densCoef * this.form.streaks.get() * this.form.opacity.get(),
                dustCoef * this.form.dust.get() * this.form.opacity.get(),
                1F, speed, wu, wv,
                (float) (flowTime % 3600.0), col.r, col.g, col.b,
                vortex ? 1F : 0F, this.form.coreRadius.get(), this.form.funnelFlare.get());
        }

        boolean wantsSway = this.form.sway.get();
        boolean wantsLeaves = this.form.leaves.get() > 0.001F && this.form.opacity.get() > 0.001F;

        int radius = Math.round(this.form.scanRadius.get());
        double ox = 0, oy = 0, oz = 0;

        if (!shadow && (wantsSway || wantsLeaves))
        {
            net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
            net.minecraft.util.math.Vec3d cam = camera.getPos();
            Vector3f rel = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix())
                .mul(context.stack.peek().getPositionMatrix())
                .transformPosition(new Vector3f(0F, 0F, 0F));
            new org.joml.Matrix4f()
                .rotateX((float) Math.toRadians(camera.getPitch()))
                .rotateY((float) Math.toRadians(camera.getYaw() + 180F))
                .invert()
                .transformPosition(rel);

            ox = cam.x + rel.x;
            oy = cam.y + rel.y;
            oz = cam.z + rel.z;

            this.fixX = (float) (LezyWindFoliage.scanCx() - ox);
            this.fixY = (float) (LezyWindFoliage.scanCy() - oy);
            this.fixZ = (float) (LezyWindFoliage.scanCz() - oz);
        }

        if (wantsSway)
        {
            if (!shadow)
            {
                LezyWindFoliage.ensure(this.form, ox, oy, oz, radius);
            }

            ExplosionFoliageVAO vao = ExplosionFoliageVAO.of(LezyWindFoliage.proxyBlocks());

            if (vao != null)
            {
                boolean vx = this.form.isVortex();

                float driver = vx ? this.form.swirl.get() : this.form.strength.get();
                float windScale = Math.max(0F, driver / (vx ? 12F : 6F));

                float raw = this.form.swayAmount.get() * windScale * gust;
                float cap = 28F;
                float swayDeg = cap * (1F - (float) Math.exp(-raw / cap));
                float rate = Math.max(0.25F, Math.min(3F, windScale));

                context.stack.push();
                context.stack.translate(this.fixX, this.fixY, this.fixZ);

                vao.renderWind(context.stack, flowTime, wu, wv, swayDeg,
                    1F, 1F, 1F, 1F, context.light, context.overlay,
                    vx, this.form.coreRadius.get(), this.form.swirl.get(),
                    this.form.suction.get(), this.form.updraft.get(), rate);

                context.stack.pop();
            }
        }
        else if (!shadow && LezyWindFoliage.isActive())
        {
            LezyWindFoliage.restore();
        }

        if (wantsLeaves && !shadow && tFlow != this.lastLeafFlow)
        {
            this.lastLeafFlow = tFlow;

            float leafScale = Math.max(0.05F, this.form.strength.get() / 6F);

            LezyWindLeaves.render(context.stack, ox, oy, oz, wu, wv, flowTime,
                this.form.leaves.get(), this.form.opacity.get(), radius,
                this.form.isVortex(), this.form.coreRadius.get(), this.form.swirl.get(),
                this.form.suction.get(), this.form.updraft.get(), leafScale);
        }
    }
}
