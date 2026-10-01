package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.utils.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UITrackpad.class, remap = false)
public abstract class UITrackpadMixin
{
    /**
     * Extend edge-wrap cooldown from 30ms to 150ms. On Linux (XWayland / Wayland), the
     * X11 event round-trip after Window.moveCursor() can take more than 30ms. The default
     * 30ms timer expires before the event finishes, causing BBS to think the cursor is
     * still stuck at the edge and repeatedly add width to shiftX, blasting the value to 7000+.
     * Combined with synchronous mouse position syncing in WindowMixin, a 150ms cooldown
     * gives the cursor time to wrap cleanly without multiple triggers.
     */
    @Redirect(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/utils/Timer;mark()V"))
    private void bbslezy$extendedCooldownOnWrap(Timer timer)
    {
        timer.mark(150L);
    }
}
