package leji.bbslezy.mixin.client;

import leji.bbslezy.utils.LezyLinuxCursor;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Linux Premiere Pro / DaVinci Resolve style scrubber for UISliderTrackpad:
 * Cursor is hidden while sliding values and allows infinite drag across
 * boundaries, then reappears right back at the click position upon release.
 * Windows remains 100% vanilla.
 */
@Mixin(value = UISliderTrackpad.class, remap = false)
public abstract class UISliderTrackpadMixin
{
    @Shadow
    protected boolean dragging;

    @Shadow
    protected int initialX;

    private boolean bbslezy$isHolding;
    private int bbslezy$origWinX;
    private int bbslezy$origWinY;
    private int bbslezy$lastMouseX;
    private int bbslezy$accumulatedDx;

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void bbslezy$beforeRender(UIContext context, CallbackInfo ci)
    {
        if (LezyOS.isWindows())
        {
            return;
        }

        if (this.dragging && Window.isMouseButtonPressed(0))
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
            }
        }
        else if (this.bbslezy$isHolding)
        {
            this.bbslezy$stopHolding();
        }
    }

    @ModifyArg(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/UISliderTrackpad;updateDragging(I)V"),
        index = 0
    )
    private int bbslezy$modifyMouseX(int mouseX)
    {
        if (LezyOS.isWindows() || !this.bbslezy$isHolding)
        {
            return mouseX;
        }

        return this.initialX + this.bbslezy$accumulatedDx;
    }

    @Inject(method = "subMouseReleased(Lmchorse/bbs_mod/ui/framework/UIContext;)Z", at = @At("RETURN"))
    private void bbslezy$afterMouseReleased(UIContext context, CallbackInfoReturnable<Boolean> cir)
    {
        if (this.bbslezy$isHolding)
        {
            this.bbslezy$stopHolding();
        }
    }

    @Inject(method = "cancelDragging()V", at = @At("HEAD"))
    private void bbslezy$onCancelDragging(CallbackInfo ci)
    {
        if (this.bbslezy$isHolding)
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
