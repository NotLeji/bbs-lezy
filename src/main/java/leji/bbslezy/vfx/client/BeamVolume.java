package leji.bbslezy.vfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen-space raymarched energy BEAM column — a fullscreen additive raymarch through a warm-core / orange-
 * rim energy field with helix ribbons, depth-occluded (railgun/nuke technique, same deferred infra as
 * {@link DomeVolume}/{@link SmokeVolume}). Replaces the beam's self-overlapping translucent core geometry
 * with a clean depth-correct volume. The column base + axis + radial basis come from the beam's draw matrix.
 */
public final class BeamVolume
{
    public static ShaderProgram PROGRAM;
    private static ShaderProgram lastProgram;
    private static Uniform uInvProj, uBase, uAxis, uTanU, uTanV, uLen, uRadius;
    private static Uniform uSoftness, uOpacity, uEmit, uHelixOn, uHelixCount, uHelixRadius;
    private static Uniform uHelixTurns, uHelixThickness, uHelixSpin, uTime, uColor, uRim, uSteps;

    private static void checkUniforms()
    {
        if (PROGRAM != lastProgram && PROGRAM != null)
        {
            lastProgram = PROGRAM;
            uInvProj = PROGRAM.getUniformOrDefault("uInvProj");
            uBase = PROGRAM.getUniformOrDefault("uBase");
            uAxis = PROGRAM.getUniformOrDefault("uAxis");
            uTanU = PROGRAM.getUniformOrDefault("uTanU");
            uTanV = PROGRAM.getUniformOrDefault("uTanV");
            uLen = PROGRAM.getUniformOrDefault("uLen");
            uRadius = PROGRAM.getUniformOrDefault("uRadius");
            uSoftness = PROGRAM.getUniformOrDefault("uSoftness");
            uOpacity = PROGRAM.getUniformOrDefault("uOpacity");
            uEmit = PROGRAM.getUniformOrDefault("uEmit");
            uHelixOn = PROGRAM.getUniformOrDefault("uHelixOn");
            uHelixCount = PROGRAM.getUniformOrDefault("uHelixCount");
            uHelixRadius = PROGRAM.getUniformOrDefault("uHelixRadius");
            uHelixTurns = PROGRAM.getUniformOrDefault("uHelixTurns");
            uHelixThickness = PROGRAM.getUniformOrDefault("uHelixThickness");
            uHelixSpin = PROGRAM.getUniformOrDefault("uHelixSpin");
            uTime = PROGRAM.getUniformOrDefault("uTime");
            uColor = PROGRAM.getUniformOrDefault("uColor");
            uRim = PROGRAM.getUniformOrDefault("uRim");
            uSteps = PROGRAM.getUniformOrDefault("uSteps");
        }
    }

    private record Entry(Matrix4f mat, float len, float radius, float softness, float opacity, float emit,
        int helixOn, int helixCount, float helixRadius, float helixTurns, float helixThickness, float helixSpin,
        float st, float colR, float colG, float colB, float rimR, float rimG, float rimB)
    {}

    private static final List<Entry> QUEUE = new ArrayList<>();

    private static Framebuffer scratch;

    private static int depthFbo = -1;
    private static int depthTex = -1;
    private static int depthW;
    private static int depthH;
    private static boolean depthReady;
    private static final Matrix4f PROJ = new Matrix4f();

    private BeamVolume()
    {}

    public static void queue(Matrix4f mat, float len, float radius, float softness, float opacity, float emit,
        boolean helixOn, int helixCount, float helixRadius, float helixTurns, float helixThickness, float helixSpin,
        float st, float colR, float colG, float colB, float rimR, float rimG, float rimB)
    {
        /* Deferred — never queue from a foreign pass (see {@link LezyVfxForeignPass}). */
        if (LezyVfxForeignPass.isActive())
        {
            return;
        }

        QUEUE.add(new Entry(new Matrix4f(mat), len, radius, softness, opacity, emit,
            helixOn ? 1 : 0, helixCount, helixRadius, helixTurns, helixThickness, helixSpin,
            st, colR, colG, colB, rimR, rimG, rimB));
    }

    public static void captureDepth(WorldRenderContext context)
    {
        if (QUEUE.isEmpty())
        {
            return;
        }

        net.minecraft.client.gl.Framebuffer main = MinecraftClient.getInstance().getFramebuffer();

        if (main == null)
        {
            return;
        }

        int w = main.textureWidth;
        int h = main.textureHeight;

        if (w <= 0 || h <= 0)
        {
            return;
        }

        ensureDepth(w, h);

        int boundFbo = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int src = boundFbo != 0 ? boundFbo : main.fbo;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthFbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, boundFbo);

        PROJ.set(RenderSystem.getProjectionMatrix());
        depthReady = true;
    }

    public static void render(net.minecraft.client.gl.Framebuffer main)
    {
        if (QUEUE.isEmpty())
        {
            return;
        }

        List<Entry> entries = new ArrayList<>(QUEUE);

        QUEUE.clear();

        boolean depth = depthReady;

        depthReady = false;

        if (PROGRAM == null || main == null || !depth)
        {
            return;
        }

        int w = main.textureWidth;
        int h = main.textureHeight;

        if (w <= 0 || h <= 0)
        {
            return;
        }

        ensureScratch(w, h);

        Matrix4f invProj = new Matrix4f(PROJ).invert();

        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

        for (Entry e : entries)
        {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scratch.id);
            GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);

            main.beginWrite(true);

            RenderSystem.colorMask(true, true, true, true);
            GL11.glColorMask(true, true, true, true);
            GlStateManager._disableScissorTest();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, scratch.getMainTexture().id);
            RenderSystem.setShaderTexture(1, depthTex);
            RenderSystem.setShader(() -> PROGRAM);

            Vector3f base = new Vector3f(e.mat.m30(), e.mat.m31(), e.mat.m32());
            Vector3f axis = new Vector3f(e.mat.m10(), e.mat.m11(), e.mat.m12()).normalize();
            Vector3f tanU = new Vector3f(e.mat.m00(), e.mat.m01(), e.mat.m02()).normalize();
            Vector3f tanV = new Vector3f(e.mat.m20(), e.mat.m21(), e.mat.m22()).normalize();

            checkUniforms();
            if (uInvProj != null) uInvProj.set(invProj);
            if (uBase != null) uBase.set(base.x, base.y, base.z);
            if (uAxis != null) uAxis.set(axis.x, axis.y, axis.z);
            if (uTanU != null) uTanU.set(tanU.x, tanU.y, tanU.z);
            if (uTanV != null) uTanV.set(tanV.x, tanV.y, tanV.z);
            if (uLen != null) uLen.set(e.len);
            if (uRadius != null) uRadius.set(e.radius);
            if (uSoftness != null) uSoftness.set(Math.max(0.05F, e.softness));
            if (uOpacity != null) uOpacity.set(e.opacity);
            if (uEmit != null) uEmit.set(e.emit);
            if (uHelixOn != null) uHelixOn.set(e.helixOn);
            if (uHelixCount != null) uHelixCount.set(e.helixCount);
            if (uHelixRadius != null) uHelixRadius.set(e.helixRadius);
            if (uHelixTurns != null) uHelixTurns.set(e.helixTurns);
            if (uHelixThickness != null) uHelixThickness.set(e.helixThickness);
            if (uHelixSpin != null) uHelixSpin.set(e.helixSpin);
            if (uTime != null) uTime.set(e.st);
            if (uColor != null) uColor.set(e.colR, e.colG, e.colB);
            if (uRim != null) uRim.set(e.rimR, e.rimG, e.rimB);
            if (uSteps != null) uSteps.set(LezyVfxQuality.getBeamSteps());

            BufferBuilder buffer = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

            LezyVfxRenderCompat.next(buffer.vertex(-1F, -1F, 0F).texture(0F, 0F));
            LezyVfxRenderCompat.next(buffer.vertex(1F, -1F, 0F).texture(1F, 0F));
            LezyVfxRenderCompat.next(buffer.vertex(1F, 1F, 0F).texture(1F, 1F));
            LezyVfxRenderCompat.next(buffer.vertex(-1F, 1F, 0F).texture(0F, 1F));
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
    }

    private static void ensureScratch(int w, int h)
    {
        if (scratch == null)
        {
            scratch = new Framebuffer();

            Texture color = new Texture();

            color.setSize(2, 2);
            color.setFilter(GL11.GL_LINEAR);
            color.setWrap(GL13.GL_CLAMP_TO_EDGE);

            scratch.attach(color, GL30.GL_COLOR_ATTACHMENT0);
            scratch.unbind();
        }

        if (scratch.getMainTexture().width != w || scratch.getMainTexture().height != h)
        {
            scratch.resize(w, h);
        }
    }

    private static void ensureDepth(int w, int h)
    {
        if (depthFbo == -1)
        {
            depthFbo = GL30.glGenFramebuffers();
        }

        if (depthTex == -1)
        {
            depthTex = GlStateManager._genTexture();
        }

        if (depthW != w || depthH != h)
        {
            depthW = w;
            depthH = h;

            GlStateManager._bindTexture(depthTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, w, h, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            GlStateManager._bindTexture(0);

            int prev = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTex, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
        }
    }
}
