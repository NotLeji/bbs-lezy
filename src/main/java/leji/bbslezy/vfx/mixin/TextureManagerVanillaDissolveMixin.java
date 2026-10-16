package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.BbsVfxAtlasDissolve;
import leji.bbslezy.vfx.client.SmearRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps block atlas for dissolve-holed variant during block form smears.
 */
@Mixin(TextureManager.class)
public abstract class TextureManagerVanillaDissolveMixin
{
    @Inject(method = "getTexture(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/texture/AbstractTexture;", at = @At("RETURN"), cancellable = true)
    private void bbslezy$dissolveBlockAtlas(Identifier id, CallbackInfoReturnable<AbstractTexture> cir)
    {
        if (!SmearRenderState.active || SmearRenderState.dissolve <= 0F
            || !PlayerScreenHandler.BLOCK_ATLAS_TEXTURE.equals(id))
        {
            return;
        }

        AbstractTexture original = cir.getReturnValue();
        AbstractTexture holed = BbsVfxAtlasDissolve.get(original, SmearRenderState.dissolve);

        if (holed != original)
        {
            cir.setReturnValue(holed);
        }
    }
}
