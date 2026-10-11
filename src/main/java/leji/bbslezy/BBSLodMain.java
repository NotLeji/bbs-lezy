package leji.bbslezy;

import leji.bbslezy.vfx.DestructionWand;
import net.fabricmc.api.ModInitializer;

public class BBSLodMain implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        DestructionWand.register();
    }
}
