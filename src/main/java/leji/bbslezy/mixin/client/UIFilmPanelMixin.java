package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyReplayActions;
import leji.bbslezy.ui.LezyIrisHelper;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Reset Replay" in the camera editor's timeline right-click menu, mirroring BBS's own
 * additions to the same clips context (see UIFilmPanel's constructor).
 */
@Mixin(value = UIFilmPanel.class, remap = false)
public abstract class UIFilmPanelMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addCameraEditorReset(CallbackInfo ci)
    {
        UIFilmPanel self = (UIFilmPanel) (Object) this;

        try
        {
            UIIcon shaderButton = new UIIcon(
                () -> LezyIrisHelper.areShadersEnabled() ? Icons.SUN : Icons.LIGHT,
                (b) -> LezyIrisHelper.toggleShaders()
            );
            shaderButton.tooltip(L10n.lang("bbslezy.ui.dashboard.shader_toggle"));
            self.actions().layout(shaderButton, LezyIrisHelper::areShadersEnabled);
        }
        catch (Throwable ignored)
        {}

        try
        {
            self.cameraEditor.clips.context((ContextMenuManager menu) ->
            {
                if (self.getData() == null)
                {
                    return;
                }

                menu.action(Icons.REFRESH, L10n.lang("bbslezy.ui.replays.reset_replay"), () ->
                {
                    try
                    {
                        LezyReplayActions.resetReplays(self);
                    }
                    catch (Throwable ignored)
                    {}
                });
            });
        }
        catch (Throwable ignored)
        {}
    }

    @Inject(method = "enterEditing", at = @At("TAIL"))
    private void bbslezy$onEnterEditing(CallbackInfo ci)
    {
        leji.bbslezy.ui.LezyPreviewSnap.onEnter((UIFilmPanel) (Object) this);
    }

    @Inject(method = "leaveEditing", at = @At("TAIL"))
    private void bbslezy$onLeaveEditing(CallbackInfo ci)
    {
        leji.bbslezy.ui.LezyPreviewSnap.onLeave((UIFilmPanel) (Object) this);
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void bbslezy$onUpdate(CallbackInfo ci)
    {
        leji.bbslezy.ui.LezyPreviewSnap.onUpdate((UIFilmPanel) (Object) this);
    }
}
