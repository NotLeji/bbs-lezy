package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.BbsVfxDissolveTexture;
import leji.bbslezy.vfx.client.SmearRenderState;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.resources.Link;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps textures for dissolve-holed variants during dissolving smear copies.
 */
@Mixin(value = TextureManager.class, remap = false)
public abstract class TextureManagerDissolveMixin
{
    @Inject(
        method = "getTexture(Lmchorse/bbs_mod/resources/Link;IZ)Lmchorse/bbs_mod/graphics/texture/Texture;",
        at = @At("RETURN"),
        cancellable = true)
    private void bbslezy$dissolveTexture(Link link, int filter, boolean silent, CallbackInfoReturnable<Texture> cir)
    {
        if (!SmearRenderState.active || SmearRenderState.dissolve <= 0F)
        {
            return;
        }

        Texture original = cir.getReturnValue();
        Texture holed = BbsVfxDissolveTexture.get(link, SmearRenderState.dissolve, original);

        if (holed != original)
        {
            cir.setReturnValue(holed);
        }
    }
}
