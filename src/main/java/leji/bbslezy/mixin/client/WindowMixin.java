package leji.bbslezy.mixin.client;

import leji.bbslezy.utils.LezyLinuxCursor;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.graphics.window.Window;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Window.class, remap = false)
public abstract class WindowMixin
{
    /**
     * On Linux/XWayland, GLFW's own cursor positioning does not reliably move
     * the hardware pointer (diagnostic evidence: vanilla UITrackpad wraps fired
     * but the cursor visibly stayed stuck). Force the physical warp via X11 root
     * window and sync Mouse.x/y so the next UI frame sees the wrapped position.
     * Windows is untouched.
     */
    @Inject(method = "moveCursor(II)V", at = @At("TAIL"))
    private static void bbslezy$afterMoveCursor(int x, int y, CallbackInfo ci)
    {
        if (LezyOS.isWindows())
        {
            return;
        }

        try
        {
            LezyLinuxCursor.warpWindow(x, y);

            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && mc.mouse instanceof MouseAccessor accessor)
            {
                accessor.bbslezy$setX(x);
                accessor.bbslezy$setY(y);
            }
        }
        catch (Throwable ignored)
        {}
    }
}
