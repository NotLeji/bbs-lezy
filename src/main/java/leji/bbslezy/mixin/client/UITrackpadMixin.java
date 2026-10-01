package leji.bbslezy.mixin.client;

import leji.bbslezy.utils.LezyLinuxCursor;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Linux Premiere Pro / DaVinci Resolve style scrubber for UITrackpad:
 * Cursor is hidden while scrubbing values and allows infinite drag across
 * boundaries, then reappears right back at the click position upon release.
 * Windows remains 100% vanilla.
 */
@Mixin(value = UITrackpad.class, remap = false)
public abstract class UITrackpadMixin extends UINumericInput<UITrackpad>
{
    @Shadow
    private boolean dragging;

    @Shadow
    private int initialX;

    @Shadow
    private int shiftX;

    private boolean bbslezy$isHolding;
    private int bbslezy$origWinX;
    private int bbslezy$origWinY;
    private int bbslezy$lastMouseX;
    private int bbslezy$accumulatedDx;

    /**
     * Disable BBS's physical edge-wrapping moveCursor calls on Linux, preventing
     * teleportation feedback loops and runaway 7000 value jumps.
     */
    @Redirect(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/graphics/window/Window;moveCursor(II)V"))
    private void bbslezy$noMoveCursorOnLinux(int x, int y)
    {
        if (LezyOS.isWindows())
        {
            Window.moveCursor(x, y);
        }
    }

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void bbslezy$beforeRender(UIContext context, CallbackInfo ci)
    {
        if (LezyOS.isWindows())
        {
            return;
        }

        boolean active = this.isDraggingTime() && Window.isMouseButtonPressed(0);

        if (active)
        {
            if (!this.bbslezy$isHolding)
            {
                this.bbslezy$isHolding = true;
                MinecraftClient mc = MinecraftClient.getInstance();
                this.bbslezy$origWinX = (int) mc.mouse.getX();
                this.bbslezy$origWinY = (int) mc.mouse.getY();
                this.bbslezy$lastMouseX = context.mouseX;
                this.bbslezy$accumulatedDx = 0;

                Window.setCursorHidden(this, true);
            }
            else
            {
                int delta = context.mouseX - this.bbslezy$lastMouseX;
                this.bbslezy$lastMouseX = context.mouseX;
                this.bbslezy$accumulatedDx += delta;

                this.shiftX = this.bbslezy$accumulatedDx;
                this.initialX = context.mouseX;
            }
        }
        else if (this.bbslezy$isHolding)
        {
            this.bbslezy$stopHolding();
        }
    }

    @Inject(method = "subMouseReleased(Lmchorse/bbs_mod/ui/framework/UIContext;)Z", at = @At("RETURN"))
    private void bbslezy$afterMouseReleased(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (this.bbslezy$isHolding)
        {
            this.bbslezy$stopHolding();
        }
    }

    @Inject(method = "subMouseClicked(Lmchorse/bbs_mod/ui/framework/UIContext;)Z", at = @At("RETURN"))
    private void bbslezy$afterMouseClicked(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (!this.dragging && this.bbslezy$isHolding)
        {
            this.bbslezy$stopHolding();
        }
    }

    private void bbslezy$stopHolding()
    {
        this.bbslezy$isHolding = false;
        Window.setCursorHidden(this, false);

        LezyLinuxCursor.warpWindow(this.bbslezy$origWinX, this.bbslezy$origWinY);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.mouse instanceof MouseAccessor accessor)
        {
            accessor.bbslezy$setX(this.bbslezy$origWinX);
            accessor.bbslezy$setY(this.bbslezy$origWinY);
        }
    }
}
