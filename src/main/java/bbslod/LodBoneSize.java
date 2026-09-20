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

    public static boolean culled(MatrixStack stack, ModelGroup group, float threshold, float viewHalfWidth, float viewHalfHeight)
    {
        if (!group.getGeometryBounds(min, max))
        {
            return false;
        }

        center.set(min).add(max).mul(0.5F * PIXEL);
        extents.set(max).sub(min).mul(0.5F * PIXEL);

        float boneWorldX = LodState.formX + center.x;
        float boneWorldY = LodState.formY + center.y;
        float boneWorldZ = LodState.formZ + center.z;

        float dx = boneWorldX - LodState.cameraX;
        float dy = boneWorldY - LodState.cameraY;
        float dz = boneWorldZ - LodState.cameraZ;

        /* The look vector points forward. The dot product gives the distance along the camera's
         * view axis (equivalent to -Z in view space). If it's negative, the bone is behind the
         * camera and we cull it immediately. */
        float depth = dx * LodState.cameraLookX + dy * LodState.cameraLookY + dz * LodState.cameraLookZ;

        if (depth < 0.001F)
        {
            return true;
        }

        Matrix4f matrix = stack.peek().getPositionMatrix();
        matrix.transformDirection(extents);
        float radius = extents.length();

        float projectedRadius = radius / depth;

        return projectedRadius / viewHalfHeight < threshold;
    }
}
