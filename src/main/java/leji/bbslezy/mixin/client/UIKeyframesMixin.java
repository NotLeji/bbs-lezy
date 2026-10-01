package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyReplayActions;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Reset Replay" in the replay actor tab's dope sheet, so a dead or displaced cast can be put
 * back without leaving the keyframes. BBS rebuilds this view on every channel list change, so
 * the entry is added in the constructor and re-attaches itself automatically.
 */
@Mixin(value = UIKeyframes.class, remap = false)
public abstract class UIKeyframesMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addResetReplay(CallbackInfo ci)
    {
        UIKeyframes self = (UIKeyframes) (Object) this;

        try
        {
            self.context((ContextMenuManager menu) ->
            {
                UIFilmPanel panel = self.getParent(UIFilmPanel.class);

                if (panel == null || panel.getData() == null)
                {
                    return;
                }

                /* This view is also reused outside the replay editor (camera clips), so the
                 * entry only makes sense on the replay actor dope sheet. */
                UIKeyframeEditor editor = panel.replayEditor.keyframeEditor;

                if (editor == null || editor.view != self)
                {
                    return;
                }

                menu.action(Icons.REFRESH, L10n.lang("bbslezy.ui.replays.reset_replay"), () ->
                {
                    try
                    {
                        LezyReplayActions.resetReplays(panel);
                    }
                    catch (Throwable ignored)
                    {}
                });
            });
        }
        catch (Throwable ignored)
        {}
    }
}
