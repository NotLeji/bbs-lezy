package bbslod;

import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Which bones survive tier 1, decided by what the BBS camera actually sees of them.
 *
 * <p>The old rule culled by hierarchy depth, which orders a rig from the hips outward and so
 * dismantled an actor in a fixed sequence — a distant figure kept its feet while losing its head,
 * the opposite of what a shot needs. A bone's geometry box, projected into the camera's view
 * space, says instead how much of the frame it occupies: a bone that is behind the camera, outside
 * its frustum, or smaller than a fraction of the view height is not worth drawing, however deep or
 * shallow it sits. Big camera-facing parts — torso, head — outlast fingers and accessories.</p>
 *
 * <p>The box comes from {@link ModelGroup#getGeometryBounds} (pixels, while the bone's frame is
 * blocks, hence the scale) and the view space from the render stack the mixin already holds. View
 * half-extents in tangent units are handed in per frame by the engine from the camera's projection
 * matrix; an orthographic or unset projection reports zero and the rule stays out of the way.</p>
 */
public class LodBoneSize
{
    private static final float PIXEL = 1F / 16F;

    private static final Vector3f min = new Vector3f();
    private static final Vector3f max = new Vector3f();
    private static final Vector3f center = new Vector3f();
    private static final Vector3f extents = new Vector3f();

    /**
     * Whether this bone is invisible enough to skip: behind the camera, clear of its frustum, or
     * projecting smaller than {@code threshold} of the view's half height.
     */
    public static boolean culled(MatrixStack stack, ModelGroup group, float threshold, float viewHalfWidth, float viewHalfHeight)
    {
        if (!group.getGeometryBounds(min, max))
        {
            return false;
        }

        center.set(min).add(max).mul(0.5F * PIXEL);
        extents.set(max).sub(min).mul(0.5F * PIXEL);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        matrix.transformPosition(center);
        matrix.transformDirection(extents);

        /* View space looks down negative Z, so anything at or behind the camera never projects. */
        if (center.z >= 0F)
        {
            return true;
        }

        float depth = -center.z;
        float radius = extents.length();
        float projectedX = Math.abs(center.x) / depth;
        float projectedY = Math.abs(center.y) / depth;
        float projectedRadius = radius / depth;

        /* Clear of the frustum on either axis — the bone's nearest point is already off frame. */
        if (projectedX - projectedRadius > viewHalfWidth || projectedY - projectedRadius > viewHalfHeight)
        {
            return true;
        }

        return projectedRadius / viewHalfHeight < threshold;
    }
}
