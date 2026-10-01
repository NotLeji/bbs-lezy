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
     * Edge-wrapping relies on Window.moveCursor() warping the physical mouse to the opposite
     * window edge. On Linux (Wayland / XWayland compositors like KDE KWin), normal-mode pointer
     * warping is blocked by compositor security policies. When moveCursor fails silently, the
     * mouse remains at the edge while BBS infinitely accumulates +width every frame into shiftX,
     * instantly blasting the value to 7000+.
     *
     * Only allow edge-wrapping on Windows where SetCursorPos actually warps the hardware pointer.
     */
    @Redirect(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/utils/Timer;isTime()Z"))
    private boolean bbslezy$wrapOnlyOnWindows(Timer timer)
    {
        if (!LezyOS.isWindows())
        {
            return false;
        }

        return timer.isTime();
    }
}
