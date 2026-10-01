package leji.bbslezy.mixin.client;

import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.utils.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UITrackpad.class, remap = false)
public abstract class UITrackpadMixin
{
    /**
     * Extend edge-wrap cooldown timer on Linux (XWayland / Wayland) from 30ms to 150ms.
     * On Linux, X11 event round-trips through the Wayland compositor take more than 30ms.
     * A 30ms timer expires before the warp event arrives, making BBS think the cursor is
     * still stuck at the edge and repeatedly adding +width every frame, exploding to 7000+.
     * On Windows, keep vanilla behavior (30ms).
     */
    @Redirect(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/utils/Timer;mark()V"))
    private void bbslezy$cooldownOnEdgeWrap(Timer timer)
    {
        if (LezyOS.isWindows())
        {
            timer.mark();
        }
        else
        {
            timer.mark(150L);
        }
    }
}
