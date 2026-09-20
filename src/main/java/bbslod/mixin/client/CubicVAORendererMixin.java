package bbslod.mixin.client;

import bbslod.LodBoneDepth;
import bbslod.LodBoneSize;
import bbslod.LodOcclusion;
import bbslod.LodSettings;
import bbslod.LodDebug;
import bbslod.LodState;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.CubicVAORenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tier 1: skip bones the camera cannot make use of — behind it, outside its frustum, or too small
 * on screen — plus the optional depth cap, and bones whose geometry hides behind terrain or
 * another model.
 *
 * <p>The size rule replaces a fixed depth order, which dismantled an actor hips-first: a distant
 * figure kept its feet and lost its head. Projected geometry keeps what the shot actually sees.
 * Depth remains as an optional blunt cap ({@code bone_cull_depth}, 0 = off).</p>
 *
 * <p>Returns {@code false} exactly as the method's own {@code !group.isVisible()} path does, so
 * the caller treats the group as drawn-nowhere and proceeds. Read-only — no {@link ModelGroup} or
 * {@link Model} state changes, so forms sharing one cached model never leak into each other.
 * {@code defaultRequire: 0} in the mixin config means a changed BBS target degrades the addon to
 * cull-only instead of refusing to boot.</p>
 */
@Mixin(CubicVAORenderer.class)
public abstract class CubicVAORendererMixin
{
    @Inject(method = "renderGroup", at = @At("HEAD"), cancellable = true)
    private void bbslod$skipDistantBone(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model, CallbackInfoReturnable<Boolean> cir)
    {
        int maxDepth = LodSettings.boneCullDepth.get();
        int depth = group == null ? 0 : LodBoneDepth.depth(group);

        /* The bone rules only apply while a world-replay form is on the stack: the engine pushes
         * a null form for every other pass, so previews, picking and shadow renders are inert. */
        boolean world = LodState.current() >= 1 && group != null;
        float threshold = LodSettings.boneCullSize.get();
        float halfWidth = LodState.viewHalfWidth;
        float halfHeight = LodState.viewHalfHeight;

        boolean depthCull = maxDepth > 0 && world && depth >= maxDepth;
        boolean sizeCull = !depthCull && world && threshold > 0F && halfHeight > 0F && LodBoneSize.culled(stack, group, threshold, halfWidth, halfHeight);

        /* Occlusion tests geometry the camera can otherwise see, so it never runs on a bone the
         * rules above already dropped. */
        boolean occlusionCull = !depthCull && !sizeCull && group != null && LodOcclusion.isOccluded(LodState.currentForm(), stack, group);

        /* Debug overlay: the box of every bone this mixin skips, so the culler's decisions stay
         * visible. Drawn bones ARE the model, and boxing every bone of every form choked the
         * frame: BBS flushes the lines layer on every layer switch, so each box was its own
         * draw call. */
        if (LodSettings.debug.get() && (depthCull || sizeCull || occlusionCull))
        {
            LodDebug.drawBone(stack, group, depthCull ? LodDebug.DEPTH_CULLED : sizeCull ? LodDebug.SIZE_CULLED : LodDebug.OCCLUSION_CULLED);
        }

        if (depthCull || sizeCull)
        {
            LodState.mixinHits++;
            cir.setReturnValue(false);

            return;
        }

        if (occlusionCull)
        {
            LodState.occlusionHits++;
            cir.setReturnValue(false);
        }
    }
}
