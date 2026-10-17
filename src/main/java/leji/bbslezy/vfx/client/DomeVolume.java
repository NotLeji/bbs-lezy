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
 * Screen-space raymarched energy dome — the railgun/nuke technique. The dome shell is NOT geometry; it's a
 * translucent hemisphere VOLUME reconstructed per pixel from the scene depth in a single fullscreen pass,
 * so it composites correctly against clouds, water and terrain BY DEPTH with zero sorting/OIT/MRT. It TINTS
 * (absorbs) the world seen through it and adds a glowing silhouette rim.
 *
 * <p>Same deferred infrastructure as {@link BeamHeat}: the dome queues params during the form pass; the
 * world depth + projection are snapshotted at {@code WorldRenderEvents.LAST} (complete under any renderer);
 * the fullscreen draw runs at {@code BBSRendering.onWorldRenderEnd} (after a pack's composite, so a custom
 * program is visible). Everything is done in VIEW space, so only the inverse projection is needed — the
 * dome centre and up-axis come straight from the form matrix.</p>
 */
public final class DomeVolume
{
    public static ShaderProgram PROGRAM;
    private static ShaderProgram lastProgram;
    private static Uniform uInvProj, uOrigin, uUp, uRadius, uHeightScale, uDensity;
    private static Uniform uOpacity, uRimPow, uEmit, uTime, uTint, uRim;

    private static void checkUniforms()
    {
        if (PROGRAM != lastProgram && PROGRAM != null)
        {
            lastProgram = PROGRAM;
            uInvProj = PROGRAM.getUniformOrDefault("uInvProj");
            uOrigin = PROGRAM.getUniformOrDefault("uOrigin");
            uUp = PROGRAM.getUniformOrDefault("uUp");
            uRadius = PROGRAM.getUniformOrDefault("uRadius");
            uHeightScale = PROGRAM.getUniformOrDefault("uHeightScale");
            uDensity = PROGRAM.getUniformOrDefault("uDensity");
            uOpacity = PROGRAM.getUniformOrDefault("uOpacity");
            uRimPow = PROGRAM.getUniformOrDefault("uRimPow");
            uEmit = PROGRAM.getUniformOrDefault("uEmit");
            uTime = PROGRAM.getUniformOrDefault("uTime");
            uTint = PROGRAM.getUniformOrDefault("uTint");
            uRim = PROGRAM.getUniformOrDefault("uRim");
        }
    }

    private record Entry(Matrix4f mat, float radius, float heightScale, float opacity, float density,
        float rimPow, float emit, float st, float tintR, float tintG, float tintB, float rimR, float rimG, float rimB)
    {}

    private static final List<Entry> QUEUE = new ArrayList<>();

    private static Framebuffer scratch;

    /* World depth + projection snapshotted at WorldRenderEvents.LAST. */
    private static int depthFbo = -1;
    private static int depthTex = -1;
    private static int depthW;
    private static int depthH;
    private static boolean depthReady;
    private static final Matrix4f PROJ = new Matrix4f();

    private DomeVolume()
    {}

    /** Queued from the dome's form-pass render (world only — the editor preview has no post pass). */
    public static void queue(Matrix4f mat, float radius, float heightScale, float opacity, float density,
        float rimPow, float emit, float st, float tintR, float tintG, float tintB, float rimR, float rimG, float rimB)
    {
        /* Deferred — never queue from a foreign pass (see {@link LezyVfxForeignPass}). */
        if (LezyVfxForeignPass.isActive())
        {
            return;
        }

        QUEUE.add(new Entry(new Matrix4f(mat), radius, heightScale, opacity, density, rimPow, emit, st,
            tintR, tintG, tintB, rimR, rimG, rimB));
    }

    /** Snapshot the world depth + projection at LAST (complete under any renderer — the BeamHeat rule). */
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

    /** Fullscreen raymarch composite — at {@code onWorldRenderEnd}, post-pack. */
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
            /* Re-snapshot the current frame colour before each dome (can't sample the attachment being
             * written; and a 2nd dome must see the 1st dome's result). */
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

            /* Dome centre + up axis in VIEW space, straight from the form matrix (form -> view). */
            Vector3f origin = new Vector3f(e.mat.m30(), e.mat.m31(), e.mat.m32());
            Vector3f up = new Vector3f(e.mat.m10(), e.mat.m11(), e.mat.m12()).normalize();

            checkUniforms();
            if (uInvProj != null) uInvProj.set(invProj);
            if (uOrigin != null) uOrigin.set(origin.x, origin.y, origin.z);
            if (uUp != null) uUp.set(up.x, up.y, up.z);
            if (uRadius != null) uRadius.set(e.radius);
            if (uHeightScale != null) uHeightScale.set(e.heightScale);
            if (uDensity != null) uDensity.set(e.density);
            if (uOpacity != null) uOpacity.set(e.opacity);
            if (uRimPow != null) uRimPow.set(e.rimPow);
            if (uEmit != null) uEmit.set(e.emit);
            if (uTime != null) uTime.set(e.st);
            if (uTint != null) uTint.set(e.tintR, e.tintG, e.tintB);
            if (uRim != null) uRim.set(e.rimR, e.rimG, e.rimB);

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
            color.setFilter(GL11.GL_NEAREST);
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
