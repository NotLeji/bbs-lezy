package bbslod.mixin.client;

import bbslod.LodBoneDepth;
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
 * Tier 1: skip bones deeper than the configured depth, so a distant actor loses its
 * fingers and accessories before it loses anything else — and skip bones whose geometry is
 * behind terrain or another model, whatever the distance.
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
        boolean depthCull = maxDepth > 0 && LodState.current() >= 1 && group != null && depth >= maxDepth;

        /* The occlusion test only runs while a world-replay form is on the stack: the engine
         * pushes a null form for every other pass, so previews, picking and shadow renders
         * never test a snapshot that does not describe them. */
        boolean occlusionCull = !depthCull && group != null && LodOcclusion.isOccluded(LodState.currentForm(), stack, group);

        if (LodSettings.debug.get() && LodState.currentForm() != null && group != null)
        {
            LodDebug.drawBone(stack, group, depthCull ? LodDebug.DEPTH_CULLED : occlusionCull ? LodDebug.OCCLUSION_CULLED : LodDebug.DRAWN);
        }

        if (depthCull)
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
