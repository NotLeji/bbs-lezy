package leji.bbslezy.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.lwjgl.glfw.GLFWNativeX11;

public class LezyLinuxCursor
{
    private interface LibX11 extends Library
    {
        LibX11 INSTANCE = Native.load("X11", LibX11.class);

        int XWarpPointer(Pointer display, long src_w, long dest_w, int src_x, int src_y, int src_width, int src_height, int dest_x, int dest_y);
        int XFlush(Pointer display);
    }

    private static boolean x11Available = true;

    /**
     * Relative pointer warp on X11 / XWayland via direct XWarpPointer.
     * When dest_w is 0 (None), XWarpPointer warps the pointer relative to its
     * current root coordinates by (deltaX, deltaY).
     */
    public static boolean warpRelative(int deltaX, int deltaY)
    {
        if (!x11Available || LezyOS.isWindows() || (deltaX == 0 && deltaY == 0))
        {
            return false;
        }

        try
        {
            long dpy = GLFWNativeX11.glfwGetX11Display();

            if (dpy != 0)
            {
                Pointer display = new Pointer(dpy);

                LibX11.INSTANCE.XWarpPointer(display, 0, 0, 0, 0, 0, 0, deltaX, deltaY);
                LibX11.INSTANCE.XFlush(display);

                return true;
            }
        }
        catch (Throwable t)
        {
            x11Available = false;
        }

        return false;
    }
}
