package leji.bbslezy.mixin.client;

import leji.bbslezy.LodSettings;
import leji.bbslezy.perf.ui.LezyDataKeysCache;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.manager.FolderManager;
import mchorse.bbs_mod.utils.repos.FolderManagerRepository;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.function.Consumer;

@Mixin(value = FolderManagerRepository.class, remap = false)
public class FolderManagerRepositoryMixin<T extends ValueGroup>
{
    @Shadow
    @Final
    private FolderManager<T> manager;

    @Inject(method = "requestKeys", at = @At("HEAD"), cancellable = true)
    public void bbslezy$requestKeysAsync(Consumer<Collection<String>> callback, CallbackInfo info)
    {
        if (LodSettings.ui_async_keys == null || !LodSettings.ui_async_keys.get())
        {
            return;
        }

        try
        {
            LezyDataKeysCache.requestKeys(this.manager, callback);
            info.cancel();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
