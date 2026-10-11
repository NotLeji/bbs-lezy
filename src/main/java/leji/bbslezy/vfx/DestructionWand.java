package leji.bbslezy.vfx;

import leji.bbslezy.BBSLod;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Wand item for selecting blocks to capture as Destruction Box / Explosion forms.
 * All mouse interaction (attack, use, alt+scroll, face push, HUD preview) is handled
 * via StructureWandMixin so it has full BBS Structure Wand controls.
 */
public class DestructionWand
{
    public static final Item ITEM = new Item(new Item.Settings().maxCount(1));

    public static void register()
    {
        Registry.register(Registries.ITEM, new Identifier(BBSLod.MOD_ID, "destruction_wand"), ITEM);

        /* Add to BBS creative tab ("bbs:main") */
        ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier("bbs", "main")))
            .register((entries) -> entries.add(ITEM));

        ItemTooltipCallback.EVENT.register((stack, context, lines) ->
        {
            if (stack.isOf(ITEM))
            {
                lines.add(Text.translatable("item.bbslezy.destruction_wand.tooltip.corners").formatted(Formatting.GRAY));
                lines.add(Text.translatable("item.bbslezy.destruction_wand.tooltip.faces").formatted(Formatting.GRAY));
                lines.add(Text.translatable("item.bbslezy.destruction_wand.tooltip.save").formatted(Formatting.GRAY));
            }
        });
    }
}
