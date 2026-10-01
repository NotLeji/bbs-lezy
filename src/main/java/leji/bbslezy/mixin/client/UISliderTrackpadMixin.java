package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyEdgeTravel;
import leji.bbslezy.utils.LezyOS;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Linux Edge Continuous Travel for UISliderTrackpad:
 * Allows continuous value scrubbing when dragging against the window edge
 * without requiring physical cursor warping.
 * Windows remains 100% vanilla.
 */
@Mixin(value = UISliderTrackpad.class, remap = false)
public abstract class UISliderTrackpadMixin
{
    @Shadow
    protected boolean dragging;

    private int bbslezy$edgeOffset;
    private long bbslezy$edgeStart;

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void bbslezy$beforeRender(UIContext context, CallbackInfo ci)
    {
        if (LezyOS.isWindows() || !this.dragging)
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

            this.bbslezy$edgeOffset += step;
        }
        else
        {
            this.bbslezy$edgeStart = 0;
        }
    }

    @ModifyArg(
        method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/input/UISliderTrackpad;updateDragging(I)V"),
        index = 0
    )
    private int bbslezy$modifyMouseX(int mouseX)
    {
        if (LezyOS.isWindows())
        {
            return mouseX;
        }

        return mouseX + this.bbslezy$edgeOffset;
    }

    @Inject(method = "stopDragging()V", at = @At("HEAD"))
    private void bbslezy$onStopDragging(CallbackInfo ci)
    {
        this.bbslezy$edgeOffset = 0;
        this.bbslezy$edgeStart = 0;
    }

    @Inject(method = "cancelDragging()V", at = @At("HEAD"))
    private void bbslezy$onCancelDragging(CallbackInfo ci)
    {
        this.bbslezy$edgeOffset = 0;
        this.bbslezy$edgeStart = 0;
    }
}
