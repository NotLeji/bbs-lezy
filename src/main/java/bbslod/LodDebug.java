package bbslod;

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
 * The debug overlay: each bone's geometry box, drawn where the renderer stands when it decides the
 * bone's fate, colour-coded by that decision.
 *
 * <p>Vertices are baked into the buffer in the bone's own frame at {@code vertex()} time, so the
 * pending batch is matrix-independent — Minecraft flushes the entity consumers at the end of the
 * frame, and the box lands where the bone was. {@link ModelGroup#getGeometryBounds} reports pixels
 * while the bone's frame is blocks (everything in {@code ICubicRenderer.applyGroupTransformations}
 * divides by 16), hence the scale here.</p>
 *
 * <p>Debug-only: one box per visible bone of every world-replay form. Expensive, and meant to be —
 * the point is seeing the rig. The lines layer depth-tests, so buried bones stay buried, which is
 * the occlusion feature's whole claim.</p>
 */
public class LodDebug
{
    private static final float PIXEL = 1F / 16F;

    public static final float[] DRAWN = {0.25F, 1F, 0.4F, 0.35F};
    public static final float[] DEPTH_CULLED = {1F, 0.25F, 0.25F, 0.8F};
    public static final float[] OCCLUSION_CULLED = {0.3F, 0.5F, 1F, 0.8F};

    private static final Vector3f min = new Vector3f();
    private static final Vector3f max = new Vector3f();

    /** Draw the bone's box in the given colour, or do nothing when the bone carries no geometry. */
    public static void drawBone(MatrixStack stack, ModelGroup group, float[] color)
    {
        if (!group.getGeometryBounds(min, max))
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = immediate.getBuffer(RenderLayer.getLines());
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
