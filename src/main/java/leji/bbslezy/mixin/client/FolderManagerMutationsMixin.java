package leji.bbslezy.mixin.client;

import leji.bbslezy.perf.ui.LezyDataKeysCache;
import mchorse.bbs_mod.utils.manager.FolderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FolderManager.class, remap = false)
public class FolderManagerMutationsMixin
{
    @Inject(method = "rename(Ljava/lang/String;Ljava/lang/String;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateRename(CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((FolderManager<?>) (Object) this);
    }

    @Inject(method = "delete(Ljava/lang/String;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateDelete(CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((FolderManager<?>) (Object) this);
    }

    @Inject(method = "addFolder(Ljava/lang/String;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateAddFolder(CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((FolderManager<?>) (Object) this);
    }

    @Inject(method = "renameFolder(Ljava/lang/String;Ljava/lang/String;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateRenameFolder(CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((FolderManager<?>) (Object) this);
    }

    @Inject(method = "deleteFolder(Ljava/lang/String;)Z", at = @At("TAIL"))
    public void bbslezy$invalidateDeleteFolder(CallbackInfoReturnable<Boolean> cir)
    {
        LezyDataKeysCache.invalidate((FolderManager<?>) (Object) this);
    }
}
