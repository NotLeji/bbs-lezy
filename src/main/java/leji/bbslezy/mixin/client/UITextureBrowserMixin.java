package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.skin.SkinSearchByNickPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.textures.UITextureBrowser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UITextureBrowser.class, remap = false)
public class UITextureBrowserMixin
{
    @Inject(method = "promptPlayerSkin", at = @At("HEAD"), cancellable = true)
    private void bbslezy$customPromptPlayerSkin(CallbackInfo ci)
    {
        try
        {
            UITextureBrowser self = (UITextureBrowser) (Object) this;
            SkinSearchByNickPanel panel = new SkinSearchByNickPanel(null);
            UIOverlay.addOverlay(self.getContext(), panel, 320, 265);
            ci.cancel();
        }
        catch (Throwable ignored)
        {}
    }
}
