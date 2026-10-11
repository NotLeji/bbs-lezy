package leji.bbslezy.mixin.client;

import leji.bbslezy.vfx.client.DestructionCapture;
import leji.bbslezy.vfx.client.LezyVfxIcons;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two buttons on the replay list's toolbar that jump to the top and the bottom of the list,
 * plus a wand capture button to turn the wand selection into a Destruction Box or Explosion replay actor.
 */
@Mixin(value = UIReplaysListPanel.class, remap = false)
public class ReplaysListPanelMixin
{
    private static final int SCROLL_ICON_SIZE = 20;
    private static final int SCROLL_ICON_MARGIN = 2;

    @Final
    @Shadow
    public UIReplayList replays;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addScrollButtons(CallbackInfo ci)
    {
        UIReplaysListPanel self = (UIReplaysListPanel) (Object) this;

        UIIcon toTop = new UIIcon(Icons.ARROW_UP, (b) ->
        {
            this.replays.scroll.setScroll(0);
        });

        UIIcon toBottom = new UIIcon(Icons.ARROW_DOWN, (b) ->
        {
            Scroll scroll = this.replays.scroll;
            int last = this.replays.getList().size() - 1;

            scroll.setScroll(last * scroll.scrollItemSize);
        });

        toTop.relative(self.bar).x(1F, -2 * SCROLL_ICON_SIZE - SCROLL_ICON_MARGIN).y(0).w(SCROLL_ICON_SIZE).h(20);
        toBottom.relative(self.bar).x(1F, -SCROLL_ICON_SIZE).y(0).w(SCROLL_ICON_SIZE).h(20);

        UIIcon capture = new UIIcon(LezyVfxIcons.DESTRUCTION, (b) ->
            b.getContext().replaceContextMenu((menu) ->
            {
                menu.action(LezyVfxIcons.DESTRUCTION, IKey.constant("Capture Destruction Box"), () ->
                {
                    DestructionCapture.capture(this.replays.panel.getData());
                    this.replays.refreshReplayList();
                    this.replays.panel.getController().createEntities();
                });
                menu.action(LezyVfxIcons.EXPLOSION, IKey.constant("Capture Explosion (sphere)"), () ->
                {
                    DestructionCapture.captureExplosion(this.replays.panel.getData());
                    this.replays.refreshReplayList();
                    this.replays.panel.getController().createEntities();
                });
                menu.action(LezyVfxIcons.BEAM, IKey.constant("Capture devouring Beam"), () ->
                {
                    DestructionCapture.captureBeam(this.replays.panel.getData());
                    this.replays.refreshReplayList();
                    this.replays.panel.getController().createEntities();
                });
                menu.action(LezyVfxIcons.DOME, IKey.constant("Capture levelling Dome"), () ->
                {
                    DestructionCapture.captureDome(this.replays.panel.getData());
                    this.replays.refreshReplayList();
                    this.replays.panel.getController().createEntities();
                });
                menu.action(Icons.UNDO, IKey.constant("Undo the last cut"), DestructionCapture::undo);
            }));
        capture.relative(self.bar).x(22).y(0).w(SCROLL_ICON_SIZE).h(20);

        self.bar.add(capture, toTop, toBottom);
        self.search.relative(self.bar).x(44).w(1F, -44 - 2 * SCROLL_ICON_SIZE - SCROLL_ICON_MARGIN);
    }
}
