package bbslod;

import mchorse.bbs_mod.forms.forms.Form;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Per-render tier and form stack, read by the bone mixin.
 *
 * <p>Render-thread only, and a stack rather than a plain field on purpose: {@code FormUtilsClient.render}
 * is reentrant through {@code renderBodyParts}, and {@code ModelFormRenderer} renders body parts inside
 * its own pass, so a single static field would be clobbered by the nesting.</p>
 *
 * <p>Two parallel deques rather than one of records: tiers are small ints (cached Integers, so
 * pushing allocates nothing) and forms are nullable references, and the bone culler needs both
 * at every nesting level.</p>
 */
public class LodState
{
    private static final Deque<Integer> tiers = new ArrayDeque<>();
    private static final Deque<Form> forms = new LinkedList<>();

    /** Bumped by the bone mixin every bone it skips, so silent mixin degradation is visible. */
    public static long mixinHits;
    public static long occlusionHits;

    /** The film camera's frustum in tangent units of view space (0 when the projection is not a
     * perspective matrix, which switches the bone-size rule off for that frame). */
    public static float viewHalfWidth;
    public static float viewHalfHeight;

    /** Renders view space of the camera the world is drawn through, recast into the film camera's
     * view space: a bone from the render stack, once transformed, is tested against the film
     * camera as though the shot were being filmed. Identity when the two cameras coincide or when
     * no film camera applies, so the bone rules fall back to the render camera. */
    public static final Matrix4f boneToWorld = new Matrix4f();
    /** Set while a film camera, rather than the render camera, drives this frame's bone rules -
     * see {@link bbslod.LodEngine}. */
    public static boolean filmCameraDriven;

    public static void push(int tier, Form form)
    {
        tiers.push(tier);
        forms.push(form);
    }

    public static void pop()
    {
        if (!tiers.isEmpty())
        {
            tiers.pop();
            forms.pop();
        }
    }

    /** The active LOD tier: 0 outside a world-replay form, so the bone mixin is inert in
     * the UI, picking and shadow passes without a second gate. */
    public static int current()
    {
        return tiers.isEmpty() ? 0 : tiers.peek();
    }

    /** The form being rendered, or null in passes occlusion has no business in (the stack is
     * only pushed with a form while the engine is active). */
    public static Form currentForm()
    {
        return forms.isEmpty() ? null : forms.peek();
    }
}
