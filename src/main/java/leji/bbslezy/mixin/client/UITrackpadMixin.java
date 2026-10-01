package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyEdgeTravel;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.utils.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Linux Edge Continuous Travel for UITrackpad:
 * Prevents KWin/Wayland pointer wrap bugs and 7000 value runaways by disabling
 * BBS's physical cursor-wrapping on Linux. Instead, when the cursor is held at
 * the edge of the window, values smoothly scroll continuously without cursor warping.
 * Windows remains 100% vanilla.
 */
@Mixin(value = UITrackpad.class, remap = false)
public abstract class UITrackpadMixin extends UINumericInput<UITrackpad>
{
    @Shadow
    private int shiftX;

    @Shadow
    private int initialX;

    @Shadow
    private double lastValue;

    private long bbslezy$edgeStart;

    /**
     * Disable BBS's physical cursor-wrapping timer block on Linux.
     * On Windows, returns original timer state so vanilla edge-wrapping runs.
     */
    @Redirect(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/utils/Timer;isTime()Z")
    )
    private boolean bbslezy$redirectIsTime(Timer timer)
    {
        if (LezyOS.isWindows())
        {
            return timer.isTime();
        }

        return false;
    }

    /**
     * Edge continuous travel on Linux: smoothly increment or decrement shiftX
     * while the mouse is held against the window boundary.
     */
    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("TAIL"))
    private void bbslezy$afterRender(UIContext context, CallbackInfo ci)
    {
        if (LezyOS.isWindows())
        {
            return;
        }

        boolean dragging = this.isDraggingTime();

        if (!dragging)
        {
            this.bbslezy$edgeStart = 0;

            return;
        }

        int mouseX = context.globalX(context.mouseX);
        int dir = LezyEdgeTravel.getEdgeDirection(mouseX, context.menu.width);

        if (dir != 0)
        {
            long now = System.currentTimeMillis();

            if (this.bbslezy$edgeStart == 0)
            {
                this.bbslezy$edgeStart = now;
            }

            int step = LezyEdgeTravel.computeStep(dir, now - this.bbslezy$edgeStart);

            this.shiftX += step;
        }
        else
        {
            this.bbslezy$edgeStart = 0;
        }

        if (this.isFocused())
        {
            context.unfocus();
        }

        int dx = (this.shiftX + context.mouseX) - this.initialX;

        if (dx != 0)
        {
            double value = this.getValueModifier();
            double diff = (Math.abs(dx) - 3) * value;
            double newValue = this.lastValue + (dx < 0 ? -diff : diff);

            newValue = diff < 0 ? this.lastValue : newValue;

            if (this.value != this.normalize(newValue))
            {
                if (this.delayedInput)
                {
                    this.setValue(newValue);
                }
                else
                {
                    this.setValueAndNotify(newValue);
                }
            }
        }
    }
}
