package leji.bbslezy.vfx;

import mchorse.bbs_mod.forms.structure.StructureSelection;
import net.minecraft.util.math.BlockPos;

/**
 * Adapter over BBS 2.7's {@link StructureSelection}.
 * Uses the same selection box, so selecting with either the Structure Wand or
 * Destruction Wand works seamlessly with all VFX captures.
 */
public final class DestructionSelection
{
    private DestructionSelection()
    {}

    public static boolean isComplete()
    {
        return StructureSelection.isReady();
    }

    public static void clear()
    {
        StructureSelection.clear();
    }

    public static BlockPos min()
    {
        return StructureSelection.getMin();
    }

    public static BlockPos max()
    {
        return StructureSelection.getMax();
    }
}
