package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = Window.class, remap = false)
public abstract class WindowMixin
{
    @Shadow
    @Final
    private static Set<Object> cursorHolders;

    /**
     * Safety net: if a drag is cancelled externally (window focus loss, Alt-Tab),
     * prune any stale numeric input holders so the cursor is not left hidden.
     */
    @Inject(method = "setStandardCursor(I)V", at = @At("HEAD"))
    private static void bbslezy$pruneStaleNumericHolders(int shape, CallbackInfo ci)
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
}
