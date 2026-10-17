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
 * The explosion's hero MUSHROOM as a screen-space volume (see {@code explosion_smoke.fsh}) — a true 3D
 * fractal-noise cloud replacing the billboard puff swarm: one fullscreen raymarch has no per-quad
 * orientation, no sorting and no cardboard edges when the camera moves through it.
 *
 * <p>★The FIRE is an emissive term of the same field ({@code heat}), so the hot core cools into the smoke
 * continuously — the billboard version cut its flames at a heat threshold while they were still at ~19% size
 * and popped them out in a single frame.</p>
 *
 * <p>Same deferred infra as {@link WindVolume}/{@link SmokeVolume}: queue params during the form pass,
 * snapshot depth + projection at {@code WorldRenderEvents.LAST}, march + composite at
 * {@code onWorldRenderEnd} (post-pack, so packs never alpha-test it into "black cores"). The cloud rides the
 * form matrix translated to the epicentre — no world origin.</p>
 */
public final class ExplosionSmokeVolume
{
    public static ShaderProgram PROGRAM;
    private static ShaderProgram lastProgram;
    private static Uniform uInvProj, uOrigin, uUp, uTanU, uTanV;
    private static Uniform uCapY, uCapR, uStemR, uDensity, uScale, uRise, uHeat;
    private static Uniform uEmit, uFade, uTime, uColor, uDrift, uBallR, uBallHeat;
    private static Uniform uShape, uSphereRadius, uSphereHeat, uSteps, uOctaves;

    private static void checkUniforms()
    {
        if (PROGRAM != lastProgram && PROGRAM != null)
        {
            lastProgram = PROGRAM;
            uInvProj = PROGRAM.getUniformOrDefault("uInvProj");
            uOrigin = PROGRAM.getUniformOrDefault("uOrigin");
            uUp = PROGRAM.getUniformOrDefault("uUp");
            uTanU = PROGRAM.getUniformOrDefault("uTanU");
            uTanV = PROGRAM.getUniformOrDefault("uTanV");
            uCapY = PROGRAM.getUniformOrDefault("uCapY");
            uCapR = PROGRAM.getUniformOrDefault("uCapR");
            uStemR = PROGRAM.getUniformOrDefault("uStemR");
            uDensity = PROGRAM.getUniformOrDefault("uDensity");
            uScale = PROGRAM.getUniformOrDefault("uScale");
            uRise = PROGRAM.getUniformOrDefault("uRise");
            uHeat = PROGRAM.getUniformOrDefault("uHeat");
            uEmit = PROGRAM.getUniformOrDefault("uEmit");
            uFade = PROGRAM.getUniformOrDefault("uFade");
            uTime = PROGRAM.getUniformOrDefault("uTime");
            uColor = PROGRAM.getUniformOrDefault("uColor");
            uDrift = PROGRAM.getUniformOrDefault("uDrift");
            uBallR = PROGRAM.getUniformOrDefault("uBallR");
            uBallHeat = PROGRAM.getUniformOrDefault("uBallHeat");
            uShape = PROGRAM.getUniformOrDefault("uShape");
            uSphereRadius = PROGRAM.getUniformOrDefault("uSphereRadius");
            uSphereHeat = PROGRAM.getUniformOrDefault("uSphereHeat");
            uSteps = PROGRAM.getUniformOrDefault("uSteps");
            uOctaves = PROGRAM.getUniformOrDefault("uOctaves");
        }
    }

    private record Entry(Matrix4f mat, float capY, float capR, float stemR, float density, float scale,
        float rise, float heat, float emit, float fade, float st,
        float colR, float colG, float colB, float driftX, float driftZ, float ballR, float ballHeat,
        float shape, float sphereR, float sphereHeat)
    {}

    private static final List<Entry> QUEUE = new ArrayList<>();

    private static Framebuffer scratch;

    private static int depthFbo = -1;
    private static int depthTex = -1;
    private static int handFbo = -1;
    private static int handTex = -1;
    private static int depthW;
    private static int depthH;
    private static boolean depthReady;
    private static final Matrix4f PROJ = new Matrix4f();

    private ExplosionSmokeVolume()
    {}

    /**
     * @param mat  the form matrix ALREADY translated to the epicentre, so its translation column is the
     *             epicentre in view space and its axes are the form's — the same trick the other volumes use
     *             to avoid reconstructing a world origin.
     * @param heat 0..1 emissive hot core; the fire is a term of this field, not a separate billboard layer.
     * @param shape 0 = mushroom (cap + stem), 1 = sphere (a single fireball at the epicentre — then
     *              {@code sphereR}/{@code sphereHeat} drive it and the cap/stem params are ignored).
     */
    public static void queue(Matrix4f mat, float capY, float capR, float stemR, float density, float scale,
        float rise, float heat, float emit, float fade, float st,
        float colR, float colG, float colB, float driftX, float driftZ, float ballR, float ballHeat,
        float shape, float sphereR, float sphereHeat)
    {
        /* Deferred — never queue from a foreign pass (see {@link LezyVfxForeignPass}). */
        if (LezyVfxForeignPass.isActive())
        {
            return;
        }

        QUEUE.add(new Entry(new Matrix4f(mat), capY, capR, stemR, density, scale, rise, heat, emit, fade,
            st, colR, colG, colB, driftX, driftZ, ballR, ballHeat, shape, sphereR, sphereHeat));
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

        /* Snapshot the current main depth = the first-person HAND (drawn after the world snapshot; its pass
         * cleared depth, so the background reads 1.0). The shader clips the march at it so streaks don't
         * show through the hand. */
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, handFbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);

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
            RenderSystem.setShaderTexture(2, handTex);
            RenderSystem.setShader(() -> PROGRAM);

            /* Zone base = the form's translation; basis = its (normalized) local axes, all in stack space. */
            Vector3f origin = new Vector3f(e.mat.m30(), e.mat.m31(), e.mat.m32());
            Vector3f up = new Vector3f(e.mat.m10(), e.mat.m11(), e.mat.m12()).normalize();
            Vector3f tanU = new Vector3f(e.mat.m00(), e.mat.m01(), e.mat.m02()).normalize();
            Vector3f tanV = new Vector3f(e.mat.m20(), e.mat.m21(), e.mat.m22()).normalize();

            checkUniforms();
            if (uInvProj != null) uInvProj.set(invProj);
            if (uOrigin != null) uOrigin.set(origin.x, origin.y, origin.z);
            if (uUp != null) uUp.set(up.x, up.y, up.z);
            if (uTanU != null) uTanU.set(tanU.x, tanU.y, tanU.z);
            if (uTanV != null) uTanV.set(tanV.x, tanV.y, tanV.z);
            if (uCapY != null) uCapY.set(e.capY);
            if (uCapR != null) uCapR.set(e.capR);
            if (uStemR != null) uStemR.set(e.stemR);
            if (uDensity != null) uDensity.set(e.density);
            if (uScale != null) uScale.set(e.scale);
            if (uRise != null) uRise.set(e.rise);
            if (uHeat != null) uHeat.set(e.heat);
            if (uEmit != null) uEmit.set(e.emit);
            if (uFade != null) uFade.set(e.fade);
            if (uTime != null) uTime.set(e.st);
            if (uColor != null) uColor.set(e.colR, e.colG, e.colB);
            if (uDrift != null) uDrift.set(e.driftX, e.driftZ);
            if (uBallR != null) uBallR.set(e.ballR);
            if (uBallHeat != null) uBallHeat.set(e.ballHeat);
            if (uShape != null) uShape.set(e.shape);
            if (uSphereRadius != null) uSphereRadius.set(e.sphereR);
            if (uSphereHeat != null) uSphereHeat.set(e.sphereHeat);
            if (uSteps != null) uSteps.set(LezyVfxQuality.getExplosionSteps());
            if (uOctaves != null) uOctaves.set(LezyVfxQuality.getExplosionOctaves());

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

        if (handFbo == -1)
        {
            handFbo = GL30.glGenFramebuffers();
        }

        if (handTex == -1)
        {
            handTex = GlStateManager._genTexture();
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

            /* Second depth target for the first-person hand (blitted from the main FBO at composite time). */
            GlStateManager._bindTexture(handTex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, w, h, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
            GlStateManager._bindTexture(0);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, handFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, handTex, 0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
        }
    }
}
