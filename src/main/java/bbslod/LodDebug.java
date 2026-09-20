package bbslod;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The debug overlay: the box of every bone the culler skips, plus the BBS film camera's position
 * and view frustum — both drawn where the renderer stands, colour-coded by decision.
 *
 * <p>The bone overlay covers only what the culler skips: a drawn bone is visible as the model's
 * own geometry, and boxing every bone of every form choked the frame (BBS flushes the lines layer
 * on every layer switch, so each box became its own draw call).</p>
 *
 * <p>Vertices are baked into the buffer at {@code vertex()} time, so the pending batch is
 * matrix-independent — Minecraft flushes the entity consumers later and the geometry lands where
 * it was emitted. The bone box is emitted in the bone's own frame; the camera frustum is computed
 * in world space and pushed through the form stack's inverse first, because that stack carries the
 * form's body yaw and raw offsets would rotate with it.</p>
 *
 * <p>Debug-only, and the lines layer depth-tests, so a bone or a camera behind terrain stays
 * hidden — which is the occlusion feature's whole claim.</p>
 */
public class LodDebug
{
    private static final float PIXEL = 1F / 16F;

    /** How far the frustum's far rectangle sits from the camera, in blocks. */
    private static final float FRUSTUM_DISTANCE = 24F;

    public static final float[] DEPTH_CULLED = {1F, 0.25F, 0.25F, 0.8F};
    public static final float[] OCCLUSION_CULLED = {0.3F, 0.5F, 1F, 0.8F};
    public static final float[] CAMERA = {1F, 0.85F, 0.15F, 0.8F};

    private static final Vector3f WORLD_UP = new Vector3f(0F, 1F, 0F);
    private static final Vector3f WORLD_FORWARD = new Vector3f(0F, 0F, 1F);

    private static final Vector3f min = new Vector3f();
    private static final Vector3f max = new Vector3f();

    private static final Vector3f look = new Vector3f();
    private static final Vector3f right = new Vector3f();
    private static final Vector3f up = new Vector3f();
    private static final Vector3f point = new Vector3f();
    private static final Matrix4f inverse = new Matrix4f();

    /** One camera frustum per frame; {@link #endFrame()} clears it after the film's render. */
    private static boolean cameraDrawn;

    /** The bone's geometry box in the given colour, or nothing when the bone carries no geometry. */
    public static void drawBone(MatrixStack stack, ModelGroup group, float[] color)
    {
        if (!group.getGeometryBounds(min, max))
        {
            return;
        }

        VertexConsumer consumer = lines();
        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normal = stack.peek().getNormalMatrix();

        float x0 = min.x * PIXEL;
        float y0 = min.y * PIXEL;
        float z0 = min.z * PIXEL;
        float x1 = max.x * PIXEL;
        float y1 = max.y * PIXEL;
        float z1 = max.z * PIXEL;

        edge(consumer, position, normal, color, x0, y0, z0, x1, y0, z0);
        edge(consumer, position, normal, color, x0, y1, z0, x1, y1, z0);
        edge(consumer, position, normal, color, x0, y0, z1, x1, y0, z1);
        edge(consumer, position, normal, color, x0, y1, z1, x1, y1, z1);

        edge(consumer, position, normal, color, x0, y0, z0, x0, y1, z0);
        edge(consumer, position, normal, color, x1, y0, z0, x1, y1, z0);
        edge(consumer, position, normal, color, x0, y0, z1, x0, y1, z1);
        edge(consumer, position, normal, color, x1, y0, z1, x1, y1, z1);

        edge(consumer, position, normal, color, x0, y0, z0, x0, y0, z1);
        edge(consumer, position, normal, color, x1, y0, z0, x1, y0, z1);
        edge(consumer, position, normal, color, x0, y1, z0, x0, y1, z1);
        edge(consumer, position, normal, color, x1, y1, z0, x1, y1, z1);
    }

    /**
     * The BBS film camera: four rays from its position through the corners of its view, joined at
     * a far rectangle — a pyramid of what the shot can see.
     *
     * <p>Drawn through the first world-replay form's stack of the frame, which is why the points
     * take a detour through that stack's inverse. The projection matrix is authoritative about
     * the aspect ratio when BBS renders at a custom size, and its negative m22 marks a real
     * perspective projection — an identity or ortho matrix falls back to the window.</p>
     */
    public static void drawCamera(MatrixStack stack, Camera camera)
    {
        cameraDrawn = true;

        look.set(camera.getLookDirection()).normalize();
        look.cross(WORLD_UP, right);

        if (right.lengthSquared() < 1.0e-6F)
        {
            look.cross(WORLD_FORWARD, right);
        }

        right.normalize();
        right.cross(look, up).normalize();

        Matrix4f projection = camera.projection;
        float tanHalfFov;
        float aspect;

        if (projection.m22() < 0F)
        {
            tanHalfFov = 1F / projection.m11();
            aspect = projection.m11() / projection.m00();
        }
        else
        {
            tanHalfFov = (float) Math.tan(camera.fov / 2F);

            double width = MinecraftClient.getInstance().getFramebuffer().textureWidth;
            double height = MinecraftClient.getInstance().getFramebuffer().textureHeight;

            aspect = height > 0D ? (float) (width / height) : 1F;
        }

        float reach = Math.min(camera.far, FRUSTUM_DISTANCE);
        float halfWidth = reach * tanHalfFov * aspect;
        float halfHeight = reach * tanHalfFov;

        double cx = camera.position.x;
        double cy = camera.position.y;
        double cz = camera.position.z;

        double fx = cx + look.x * reach;
        double fy = cy + look.y * reach;
        double fz = cz + look.z * reach;

        VertexConsumer consumer = lines();
        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normal = stack.peek().getNormalMatrix();

        inverse.set(position).invert();

        /* Four corners of the far rectangle, then the four rays to them, then the loop around. */
        double[] corners = {
            fx + right.x * halfWidth + up.x * halfHeight, fy + right.y * halfWidth + up.y * halfHeight, fz + right.z * halfWidth + up.z * halfHeight,
            fx - right.x * halfWidth + up.x * halfHeight, fy - right.y * halfWidth + up.y * halfHeight, fz - right.z * halfWidth + up.z * halfHeight,
            fx - right.x * halfWidth - up.x * halfHeight, fy - right.y * halfWidth - up.y * halfHeight, fz - right.z * halfWidth - up.z * halfHeight,
            fx + right.x * halfWidth - up.x * halfHeight, fy + right.y * halfWidth - up.y * halfHeight, fz + right.z * halfWidth - up.z * halfHeight,
        };

        for (int i = 0; i < 4; i++)
        {
            worldLine(consumer, position, normal, cx, cy, cz, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2]);
            int j = (i + 1) & 3;

            worldLine(consumer, position, normal, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2], corners[j * 3], corners[j * 3 + 1], corners[j * 3 + 2]);
        }
    }

    /** The frame's camera frustum has been drawn; let the next frame draw it again. */
    public static void endFrame()
    {
        cameraDrawn = false;
    }

    /** Whether this frame's camera frustum still needs drawing. */
    public static boolean cameraPending()
    {
        return !cameraDrawn;
    }

    /** One world-space line through the form stack: the endpoints are un-rotated by its inverse. */
    private static void worldLine(VertexConsumer consumer, Matrix4f position, Matrix3f normal, double ax, double ay, double az, double bx, double by, double bz)
    {
        float lax = inverse.transformPosition(point.set((float) ax, (float) ay, (float) az)).x;
        float lay = point.y;
        float laz = point.z;

        float lbx = inverse.transformPosition(point.set((float) bx, (float) by, (float) bz)).x;
        float lby = point.y;
        float lbz = point.z;

        edge(consumer, position, normal, CAMERA, lax, lay, laz, lbx, lby, lbz);
    }

    private static VertexConsumer lines()
    {
        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        return immediate.getBuffer(RenderLayer.getLines());
    }

    private static void edge(VertexConsumer consumer, Matrix4f position, Matrix3f normal, float[] color,
        float ax, float ay, float az, float bx, float by, float bz)
    {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float inv = length == 0F ? 0F : 1F / length;

        consumer.vertex(position, ax, ay, az).color(color[0], color[1], color[2], color[3]).normal(normal, dx * inv, dy * inv, dz * inv).next();
        consumer.vertex(position, bx, by, bz).color(color[0], color[1], color[2], color[3]).normal(normal, dx * inv, dy * inv, dz * inv).next();
    }
}
