package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.DestructionWand;
import mchorse.bbs_mod.forms.structure.StructureWand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Allows Destruction Wand to behave exactly like Structure Wand:
 * Full HUD hints, corner A/B picking on attack/use, Alt+scroll to resize faces, and 3D ghost box selection.
 */
@Mixin(value = StructureWand.class, remap = false)
public abstract class StructureWandMixin
{
    @Redirect(
        method = "isHolding(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z")
    )
    private static boolean bbslezy$isHoldingWand(ItemStack stack, net.minecraft.item.Item item)
    {
        return stack.isOf(item) || stack.isOf(DestructionWand.ITEM);
    }
}
