package leji.bbslezy.mixin.client;

import leji.bbslezy.utils.LezyLinuxCursor;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.graphics.window.Window;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Set;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;

@Mixin(value = Window.class, remap = false)
public abstract class WindowMixin
{
    @Shadow
    @Final
    private static Set<Object> cursorHolders;


    /**
     * On Wayland, {@code glfwSetCursorPos} only updates GLFW's virtual cursor
     * coordinates when the cursor mode is {@code GLFW_CURSOR_DISABLED}, and does
     * not fire the cursor-pos callback synchronously. Sync {@code Mouse.x/y} so
     * the next UI frame immediately sees the wrapped position.
     */
    @Inject(method = "moveCursor(II)V", at = @At("TAIL"))
    private static void bbslezy$afterMoveCursor(int x, int y, CallbackInfo ci)
    {
        try
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && mc.mouse != null && !LezyOS.isWindows())
            {
                int currentX = (int) mc.mouse.getX();
                int currentY = (int) mc.mouse.getY();
                int dx = x - currentX;
                int dy = y - currentY;

                LezyLinuxCursor.warpRelative(dx, dy);
            }

            syncMinecraftMouse(x, y);
        }
        catch (Throwable ignored)
        {}
    }


    @Inject(method = "setStandardCursor(I)V", at = @At("HEAD"))
    private static void bbslezy$pruneStaleTrackpadHolders(int shape, CallbackInfo ci)
    {
        if (cursorHolders == null || cursorHolders.isEmpty())
        {
            return;
        }

        try
        {
            if (!Window.isMouseButtonPressed(0))
            {
                boolean removed = cursorHolders.removeIf((h) -> h instanceof UINumericInput<?>);

                if (removed && cursorHolders.isEmpty() && MinecraftClient.getInstance().currentScreen != null)
                {
                    long window = Window.getWindow();

                    if (GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_NORMAL)
                    {
                        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                    }
                }
            }
        }
        catch (Throwable ignored)
        {}
    }
    private static void syncMinecraftMouse(int x, int y)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.mouse == null)
        {
            return;
        }

        try
        {
            if (mc.mouse instanceof MouseAccessor accessor)
            {
                accessor.bbslezy$setX(x);
                accessor.bbslezy$setY(y);
            }
        }
        catch (Throwable ignored)
        {}
    }
}
