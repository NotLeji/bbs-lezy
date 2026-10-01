package leji.bbslezy.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import mchorse.bbs_mod.graphics.window.Window;
import org.lwjgl.glfw.GLFWNativeX11;

public class LezyLinuxCursor
{
    private interface LibX11 extends Library
    {
        LibX11 INSTANCE = Native.load("X11", LibX11.class);

        long XDefaultRootWindow(Pointer display);

        int XTranslateCoordinates(
            Pointer display,
            long src_w,
            long dest_w,
            int src_x,
            int src_y,
            IntByReference dest_x,
            IntByReference dest_y,
            LongByReference child
        );

        int XWarpPointer(
            Pointer display,
            long src_w,
            long dest_w,
            int src_x,
            int src_y,
            int src_width,
            int src_height,
            int dest_x,
            int dest_y
        );

        int XFlush(Pointer display);
    }

    private static boolean x11Available = true;

    /**
     * Warps cursor to window-relative coordinates (x, y) on Linux via X11 root window.
     * Translating client window coordinates to desktop root coordinates and warping
     * with dest_w = RootWindow physically and reliably moves the pointer on XWayland/KWin.
     */
    public static boolean warpWindow(int x, int y)
    {
        if (!x11Available || LezyOS.isWindows())
        {
            return false;
        }

        try
        {
            long dpy = GLFWNativeX11.glfwGetX11Display();
            long win = GLFWNativeX11.glfwGetX11Window(Window.getWindow());

            if (dpy != 0 && win != 0)
            {
                Pointer display = new Pointer(dpy);
                long root = LibX11.INSTANCE.XDefaultRootWindow(display);

                IntByReference rootX = new IntByReference();
                IntByReference rootY = new IntByReference();
                LongByReference child = new LongByReference();

                int success = LibX11.INSTANCE.XTranslateCoordinates(display, win, root, 0, 0, rootX, rootY, child);

                if (success != 0)
                {
                    int targetRootX = rootX.getValue() + x;
                    int targetRootY = rootY.getValue() + y;

                    LibX11.INSTANCE.XWarpPointer(display, 0, root, 0, 0, 0, 0, targetRootX, targetRootY);
                    LibX11.INSTANCE.XFlush(display);

                    return true;
                }
            }
        }
        catch (UnsatisfiedLinkError | NoClassDefFoundError t)
        {
            x11Available = false;
        }
        catch (Throwable ignored)
        {}

        return false;
    }
}
