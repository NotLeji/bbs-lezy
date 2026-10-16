package leji.bbslezy.vfx;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gate mixins addon VFX pada presence mod pihak ketiga, supaya target lookup
 * tidak terotheran class yang tidak ada. Pola persis BbsVfxClientMixinPlugin asli.
 */
public class LezyVfxMixinPlugin implements IMixinConfigPlugin
{
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (mixinClassName.endsWith(".PhysicsModCudaMixin"))
        {
            return FabricLoader.getInstance().isModLoaded("physicsmod");
        }

        return true;
    }

    @Override
    public void onLoad(String mixinPackage)
    {
        MixinExtrasBootstrap.init();
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
