package leji.bbslezy.vfx;

import leji.bbslezy.BBSLod;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Wand item for selecting blocks to capture as Destruction Box / Explosion forms.
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

        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
        {
            if (player.getStackInHand(hand).getItem() != ITEM)
            {
                return ActionResult.PASS;
            }

            if (world.isClient)
            {
                BlockPos pos = hit.getBlockPos();

                if (player.isSneaking())
                {
                    DestructionSelection.pos1 = pos;
                }
                else
                {
                    DestructionSelection.pos2 = pos;
                }
            }

            return ActionResult.SUCCESS;
        });
    }
}
