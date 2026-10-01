package leji.bbslezy.utils;

import mchorse.bbs_mod.graphics.window.Window;

import java.util.function.Supplier;
import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.MinecraftClient;

public class LezyOS
{
    public static Supplier<String> osName = () -> System.getProperty("os.name", "");

    public static boolean isWindows()
    {
        return osName.get().toLowerCase().contains("win");
    }

    public static boolean isLinuxLike()
    {
        String os = osName.get().toLowerCase();

        return os.contains("nux") || os.contains("nix") || os.contains("aix");
    }

    public static boolean updateCursorHold(Object holder, boolean currentlyHolding, boolean shouldHold)
    {
        if (isWindows())
        {
            return false;
        }

        try
        {
            if (shouldHold)
            {
                Window.setCursorHidden(holder, true);
                return true;
            }
            else if (currentlyHolding)
            {
                Window.setCursorHidden(holder, false);
            }
        }
        catch (Throwable ignored)
        {}

        return false;
    }

    public static void warpCursorToCurrent(UIContext context)
    {
        try
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            int ww = mc.getWindow().getWidth();
            double factor = Math.ceil(ww / (double) context.menu.width);

            Window.moveCursor((int) (context.globalX(context.mouseX) * factor), (int) mc.mouse.getY());
        }
        catch (Throwable ignored)
        {}
    }

    /* Releases the hold and writes the current logical cursor position to the
     * physical pointer, overwriting GLFW's stale restore-position warp that
     * fires when the cursor leaves GLFW_CURSOR_DISABLED. Order matters: flip
     * to normal first (that is what triggers the stale warp), then move. */
    public static boolean releaseCursorHold(Object holder, boolean holding, UIContext context)
    {
        if (!holding)
        {
            return false;
        }

        boolean now = updateCursorHold(holder, holding, false);

        warpCursorToCurrent(context);

        return now;
    }
}
