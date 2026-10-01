package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyInfiniteDrag;
import leji.bbslezy.utils.LezyLinuxCursor;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DaVinci Resolve-style infinite drag for UITrackpad on Linux: while dragging,
 * the cursor is hidden (GLFW_CURSOR_DISABLED = unbounded travel) and the value
 * follows accumulated raw pointer motion; on release the cursor reappears at
 * the original click position. Windows stays 100% vanilla.
 */
@Mixin(value = UITrackpad.class, remap = false)
public abstract class UITrackpadMixin extends UINumericInput<UITrackpad>
{
    @Shadow
    private boolean dragging;

    @Unique
    private final LezyInfiniteDrag bbslezy$drag = new LezyInfiniteDrag();

    @Unique
    private static final Object bbslezy$HOLDER = new Object();

    @Unique
    private int bbslezy$origPhysX;

    @Unique
    private int bbslezy$origPhysY;

    @Unique
    private double bbslezy$factor = 1.0;

    @Unique
    private boolean bbslezy$hiding;

    /**
     * Suppress vanilla edge-wrap teleport while infinite drag is active —
     * DISABLED mode already gives unbounded travel, a warp would fight it.
     */
    @Redirect(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/graphics/window/Window;moveCursor(II)V")
    )
    private void bbslezy$suppressVanillaWrap(int x, int y)
    {
        if (LezyOS.isWindows() || !this.bbslezy$hiding)
        {
            Window.moveCursor(x, y);
        }
    }

    /**
     * Replace vanilla's edge-clamped dx with accumulated raw pointer motion
     * converted to menu units. Sole Math.abs(int) call site in render, so no
     * ordinal needed. Windows passes through untouched.
     */
    @Redirect(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I")
    )
    private int bbslezy$replaceDx(int dx)
    {
        if (LezyOS.isWindows() || !this.bbslezy$hiding)
        {
            return Math.abs(dx);
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        this.bbslezy$drag.feed(mc.mouse.getX());

        int rawDx = this.bbslezy$drag.getAccumulatedDx();
        int menuDx = (int) (rawDx / this.bbslezy$factor);

        return Math.abs(menuDx);
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void bbslezy$trackDragState(UIContext context, CallbackInfo ci)
    {
        if (LezyOS.isWindows())
        {
            return;
        }

        boolean active = this.isDraggingTime() && this.dragging && Window.isMouseButtonPressed(0);

        if (active && !this.bbslezy$hiding)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            this.bbslezy$origPhysX = (int) mc.mouse.getX();
            this.bbslezy$origPhysY = (int) mc.mouse.getY();
            this.bbslezy$drag.begin(mc.mouse.getX());
            this.bbslezy$factor = Math.ceil(mc.getWindow().getWidth() / (double) context.menu.width);
            this.bbslezy$hiding = true;
        }

        if (this.bbslezy$hiding)
        {
            if (!active)
            {
                this.bbslezy$restore();
            }
            else
            {
                Window.setCursorHidden(bbslezy$HOLDER, true);
            }
        }
    }

    @Inject(method = "subMouseReleased(Lmchorse/bbs_mod/ui/framework/UIContext;)Z", at = @At("HEAD"))
    private void bbslezy$restoreOnRelease(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (!LezyOS.isWindows() && this.bbslezy$hiding)
        {
            this.bbslezy$restore();
        }
    }

    @Inject(method = "subMouseClicked(Lmchorse/bbs_mod/ui/framework/UIContext;)Z", at = @At("HEAD"))
    private void bbslezy$restoreOnCancel(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (!LezyOS.isWindows() && this.bbslezy$hiding && context.mouseButton == 1 && this.isDragging())
        {
            this.bbslezy$restore();
        }
    }

    @Unique
    private void bbslezy$restore()
    {
        this.bbslezy$hiding = false;
        this.bbslezy$drag.end();

        try
        {
            Window.setCursorHidden(bbslezy$HOLDER, false);
            LezyLinuxCursor.warpWindow(this.bbslezy$origPhysX, this.bbslezy$origPhysY);

            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc != null && mc.mouse instanceof MouseAccessor accessor)
            {
                accessor.bbslezy$setX(this.bbslezy$origPhysX);
                accessor.bbslezy$setY(this.bbslezy$origPhysY);
            }
        }
        catch (Throwable ignored)
        {}
    }
}
