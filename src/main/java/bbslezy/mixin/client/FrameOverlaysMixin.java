package bbslezy.mixin.client;

import mchorse.bbs_mod.ui.film.FrameOverlays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Hands BBS Lezy the overlay registry, so the order of the frame's overlays can be decided per
 * timeline track instead of in the order the renderers happened to be registered.
 *
 * <p>Only the list is read and written - no BBS method is replaced and nothing is injected, so a
 * BBS that renames the field leaves this mixin out and the overlays fall back to drawing in
 * registration order, which is what they always did.</p>
 */
@Mixin(value = FrameOverlays.class, remap = false)
public abstract class FrameOverlaysMixin
{
    @Accessor("RENDERERS")
    public static List<FrameOverlays.IFrameOverlayRenderer> bbslezy$getRenderers()
    {
        throw new AssertionError("Replaced by the mixin processor");
    }
}
