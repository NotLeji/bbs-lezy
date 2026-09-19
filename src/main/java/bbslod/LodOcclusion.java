package bbslod;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.WeakHashMap;

/**
 * Per-bone occlusion culling: skip a bone whose geometry is hidden behind terrain or another
 * model, regardless of distance.
 *
 * <p>The depth buffer at the end of a film's render holds every opaque block and every form drawn
 * up to that point, so a bone that ends up behind any of it is detectable. One snapshot per frame
 * ({@link #capture()}, at {@code FilmEvents.RENDER_AFTER}) is blitted down to a small buffer and
 * read back once; the next frame's {@link #isOccluded} tests are plain array lookups.</p>
 *
 * <p>Every sample point of a bone's geometry box must agree it is hidden, and it must agree two
 * frames running, so an edge-flickering bone stays drawn. Bias is in eye-space blocks rather than
 * depth-range units, so it means the same thing at ten blocks and at a thousand.</p>
 *
 * <p>This is the addon's only OpenGL state outside BBS's own code, and it is contained: the
 * read and draw framebuffer bindings are saved and put back in a {@code finally}, because
 * Minecraft's GlStateManager caches what it believes is bound and a stale cache corrupts the
 * rest of the frame. Any failure — a uniform snapshot, an incomplete FBO, an exception —
 * disables the feature for the session rather than culling wrongly.</p>
 */
public class LodOcclusion
{
    private static final Logger LOGGER = LoggerFactory.getLogger("bbslod");

    /** Snapshot resolution is the window divided by this: lower is cheaper and coarser. */
    private static final int DOWN_SAMPLE = 4;

    /** Fully occluded frames in a row before a bone is dropped — kills edge flicker. */
    private static final int FRAMES_TO_CULL = 2;

    /* GL sized depth internal formats, given as raw ints: their homes in the LWJGL class layout
     * vary between versions, but the values are fixed by the OpenGL spec. */
    private static final int DEPTH_COMPONENT16 = 0x81A5;
    private static final int DEPTH_COMPONENT24 = 0x81A6;
    private static final int DEPTH_COMPONENT32 = 0x81A7;
    private static final int DEPTH_COMPONENT32F = 0x8CAC;

    private static boolean available = true;
    private static boolean initialised;
    private static boolean validated;

    private static int smallFbo = -1, depthTex = -1;
    private static int snapW, snapH;
    private static int mainFbo = -1, mainW, mainH;

    /** {@code back} is filled by the capture, {@code front} is what the tests read; they swap. */
    private static FloatBuffer front, back;

    /** Local-space sample points per group: geometry data is static, so this is keyed by the
     * shared group and dies with the cached model. */
    private static final WeakHashMap<ModelGroup, Vector3f[]> SAMPLES = new WeakHashMap<>();

    /** Consecutive occluded-frame count, per form per bone. Forms are per-controller copies, so
     * this state never crosses takes; {@link #reset()} clears it on film shutdown. */
    private static final IdentityHashMap<Form, IdentityHashMap<ModelGroup, Integer>> OCCLUDED = new IdentityHashMap<>();

    /* Render-thread scratch — this whole path is single threaded. */
    private static final Matrix4f scratchMvp = new Matrix4f();
    private static final Vector4f scratchVec = new Vector4f();

    public static void reset()
    {
        OCCLUDED.clear();
    }

    /**
     * Captures the depth buffer at the end of a film's render, for next frame's tests.
     */
    public static void capture()
    {
        if (!available || !LodSettings.occlusion.get())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc == null ? null : mc.getFramebuffer();

        if (main == null)
        {
            return;
        }

        try
        {
            if (!initialised || main.fbo != mainFbo || main.viewportWidth != mainW || main.viewportHeight != mainH)
            {
                init(main);
            }

            if (!available)
            {
                return;
            }

            int prevRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int prevDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

            try
            {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, smallFbo);
                GL30.glBlitFramebuffer(0, 0, mainW, mainH, 0, 0, snapW, snapH, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, smallFbo);
                GL11.glReadPixels(0, 0, snapW, snapH, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, back);
            }
            finally
            {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            }

            if (!validated)
            {
                /* A blit that silently did nothing leaves a uniform buffer, and uniform depth
                 * means everything looks occluded — detect it once, then trust. */
                if (!hasVariance(back))
                {
                    LOGGER.warn("bbslod: depth snapshot is uniform (the blit did not transfer, likely a format mismatch); occlusion culling disabled for this session");
                    available = false;

                    return;
                }

                validated = true;
            }

            FloatBuffer swap = front;
            front = back;
            back = swap;
        }
        catch (Exception e)
        {
            LOGGER.warn("bbslod: occlusion capture failed, disabling for this session ({})", e.toString());
            available = false;
        }
    }

    /**
     * Whether this bone should be skipped this frame: every sample point of its geometry box has
     * been behind something that wrote depth for {@link #FRAMES_TO_CULL} frames running.
     */
    public static boolean isOccluded(Form form, MatrixStack stack, ModelGroup group)
    {
        if (!available || !LodSettings.occlusion.get() || front == null || form == null || group == null)
        {
            return false;
        }

        Vector3f[] points = samplePoints(group);

        if (points == null)
        {
            return false;
        }

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f mvp = scratchMvp.set(projection)
            .mul(RenderSystem.getModelViewMatrix())
            .mul(stack.peek().getPositionMatrix());

        float projA = projection.m22();
        float projB = projection.m32();
        float bias = LodSettings.occlusionBias.get();
        boolean occluded = true;

        for (Vector3f point : points)
        {
            Vector4f v = scratchVec.set(point.x, point.y, point.z, 1F).mul(mvp);

            if (v.w <= 0F)
            {
                return false; /* behind the camera */
            }

            float invW = 1F / v.w;
            float ndcX = v.x * invW;
            float ndcY = v.y * invW;
            float ndcZ = v.z * invW;

            if (ndcX < -1F || ndcX > 1F || ndcY < -1F || ndcY > 1F || ndcZ < -1F || ndcZ > 1F)
            {
                return false; /* off-frustum: fail open, the snapshot has nothing to say */
            }

            int u = (int) ((ndcX * 0.5F + 0.5F) * (snapW - 1));
            int vv = (int) ((ndcY * 0.5F + 0.5F) * (snapH - 1));

            /* Eye-space distance of whatever wrote depth at that pixel, and of this bone point.
             * Both go through the same projection constants, so the bias is in blocks. */
            float occluder = distanceFromWindow(front.get(vv * snapW + u), projA, projB);
            float bone = distanceFromWindow(ndcZ * 0.5F + 0.5F, projA, projB);

            if (Float.isNaN(occluder) || Float.isNaN(bone) || bone <= occluder + bias)
            {
                return false; /* this point is visible, so the bone is */
            }
        }

        return record(form, group, occluded);
    }
    /**
     * Distance from the camera, from a window-space depth and this frame's projection — a
     * positive number, so a bone further than the occluder is a greater one.
     */
    private static float distanceFromWindow(float windowZ, float projA, float projB)
    {
        float ndcZ = windowZ * 2F - 1F;
        float denominator = ndcZ + projA;

        if (denominator == 0F)
        {
            return Float.POSITIVE_INFINITY; /* at the far plane: nothing is behind this */
        }

        return projB / denominator;
    }

    private static boolean record(Form form, ModelGroup group, boolean occludedNow)
    {
        if (!occludedNow)
        {
            IdentityHashMap<ModelGroup, Integer> map = OCCLUDED.get(form);

            if (map != null)
            {
                map.remove(group);

                if (map.isEmpty())
                {
                    OCCLUDED.remove(form);
                }
            }

            return false;
        }

        IdentityHashMap<ModelGroup, Integer> map = OCCLUDED.computeIfAbsent(form, k -> new IdentityHashMap<>());
        Integer current = map.get(group);

        /* Capped at FRAMES_TO_CULL so a permanently hidden bone stops boxing new integers. */
        int count = Math.min((current == null ? 0 : current) + 1, FRAMES_TO_CULL);

        if (current == null || current < FRAMES_TO_CULL)
        {
            map.put(group, count);
        }

        return count >= FRAMES_TO_CULL;
    }

    /** Eight corners and the centre of the bone's own geometry box, in its local space. */
    private static Vector3f[] samplePoints(ModelGroup group)
    {
        Vector3f[] cached = SAMPLES.get(group);

        if (cached != null)
        {
            return cached;
        }

        Vector3f min = new Vector3f();
        Vector3f max = new Vector3f();

        if (!group.getGeometryBounds(min, max))
        {
            return null; /* a bone with no geometry has nothing to cull */
        }

        float cx = (min.x + max.x) * 0.5F;
        float cy = (min.y + max.y) * 0.5F;
        float cz = (min.z + max.z) * 0.5F;

        cached = new Vector3f[] {
            new Vector3f(min.x, min.y, min.z), new Vector3f(max.x, min.y, min.z),
            new Vector3f(min.x, max.y, min.z), new Vector3f(max.x, max.y, min.z),
            new Vector3f(min.x, min.y, max.z), new Vector3f(max.x, min.y, max.z),
            new Vector3f(min.x, max.y, max.z), new Vector3f(max.x, max.y, max.z),
            new Vector3f(cx, cy, cz)
        };

        SAMPLES.put(group, cached);

        return cached;
    }

    private static boolean hasVariance(FloatBuffer buffer)
    {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0, n = buffer.limit(); i < n; i++)
        {
            float value = buffer.get(i);

            if (value < min) min = value;
            if (value > max) max = value;
        }

        return min < max;
    }

    private static void init(Framebuffer main) throws Exception
    {
        int depthAttachment = main.getDepthAttachment();

        if (depthAttachment <= 0)
        {
            throw new Exception("the main framebuffer has no depth attachment");
        }

        int[] format = new int[1];

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthAttachment);
        GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT, format);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        /* The blit requires matching depth formats, so the snapshot mirrors whatever Minecraft
         * uses rather than assuming a bit depth. */
        int internal = format[0];
        int type;

        if (internal == GL30.GL_DEPTH24_STENCIL8)
        {
            type = GL30.GL_UNSIGNED_INT_24_8;
        }
        else if (internal == GL30.GL_DEPTH32F_STENCIL8)
        {
            type = GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
        }
        else if (internal == GL11.GL_DEPTH_COMPONENT || internal == DEPTH_COMPONENT16
            || internal == DEPTH_COMPONENT24 || internal == DEPTH_COMPONENT32
            || internal == DEPTH_COMPONENT32F)
        {
            type = GL11.GL_FLOAT;
        }
        else
        {
            internal = DEPTH_COMPONENT24;
            type = GL11.GL_FLOAT;
        }

        snapW = Math.max(1, main.viewportWidth / DOWN_SAMPLE);
        snapH = Math.max(1, main.viewportHeight / DOWN_SAMPLE);

        dispose();

        smallFbo = GL30.glGenFramebuffers();
        depthTex = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTex);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internal, snapW, snapH, 0, GL11.GL_DEPTH_COMPONENT, type, (ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, smallFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTex, 0);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.fbo);

        if (status != GL30.GL_FRAMEBUFFER_COMPLETE)
        {
            throw new Exception("the snapshot framebuffer is incomplete (0x" + Integer.toHexString(status) + ")");
        }

        front = MemoryUtil.memAlloc(snapW * snapH * 4).asFloatBuffer();
        back = MemoryUtil.memAlloc(snapW * snapH * 4).asFloatBuffer();

        mainFbo = main.fbo;
        mainW = main.viewportWidth;
        mainH = main.viewportHeight;
        initialised = true;
        validated = false;

        LOGGER.info("bbslod: occlusion snapshot {}x{} (depth format 0x{})", snapW, snapH, Integer.toHexString(internal));
    }

    private static void dispose()
    {
        if (smallFbo != -1)
        {
            GL30.glDeleteFramebuffers(smallFbo);
            smallFbo = -1;
        }

        if (depthTex != -1)
        {
            GL11.glDeleteTextures(depthTex);
            depthTex = -1;
        }

        if (front != null)
        {
            MemoryUtil.memFree(front);
            front = null;
        }

        if (back != null)
        {
            MemoryUtil.memFree(back);
            back = null;
        }

        initialised = false;
    }
}
