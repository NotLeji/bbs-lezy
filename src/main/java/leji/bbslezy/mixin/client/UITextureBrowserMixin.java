package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.textures.UITextureBrowser;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.resources.PlayerSkins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A magnifier next to the texture browser's search box that downloads a Java player skin by
 * nickname: BBS already has the Mojang fetch and the nickname prompt, this only wires the
 * existing search text to them.
 */
@Mixin(value = UITextureBrowser.class, remap = false)
public abstract class UITextureBrowserMixin
{
    @Shadow
    private void fetchPlayerSkin(String nickname, boolean refetch)
    {}

    @Shadow
    private void promptPlayerSkin()
    {}

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addSkinSearch(CallbackInfo ci)
    {
        UITextureBrowser self = (UITextureBrowser) (Object) this;

        try
        {
            self.bar.add(new UIIcon(Icons.SEARCH, (b) ->
            {
                String nickname = self.search.getText().trim();

                if (PlayerSkins.isNickname(nickname))
                {
                    this.fetchPlayerSkin(nickname, false);
                }
                else
                {
                    this.promptPlayerSkin();
                }
            }));
        }
        catch (Throwable ignored)
        {}
    }
}
