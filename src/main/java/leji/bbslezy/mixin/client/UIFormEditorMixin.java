package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.skin.SkinSearchByNickPanel;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIFormEditor.class, remap = false)
public class UIFormEditorMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addSkinSearch(CallbackInfo ci)
    {
        try
        {
            UIFormEditor editor = (UIFormEditor) (Object) this;
            UIIcon searchSkin = new UIIcon(Icons.SEARCH, b ->
            {
                SkinSearchByNickPanel panel = new SkinSearchByNickPanel(editor);
                UIOverlay.addOverlay(editor.getContext(), panel, 320, 265);
            });
            searchSkin.tooltip(L10n.lang("bbslezy.ui.skin.by_nickname"));
            editor.icons.add(searchSkin);
            editor.icons.resize();
        }
        catch (Throwable ignored)
        {}
    }
}
