package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.utils.Timer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Edge-wrapping for UISliderTrackpad, mirroring vanilla UITrackpad logic 1:1:
 * identical trigger coordinates, identical targets, identical dx continuity
 * adjustment. Fires on the same frame as updateDragging so the wrapped delta
 * stays continuous.
 */
@Mixin(value = UISliderTrackpad.class, remap = false)
public abstract class UISliderTrackpadMixin
{
    @Shadow
    protected boolean dragging;

    @Shadow
    protected int initialX;

    private final Timer bbslezy$wrapTimer = new Timer(30L);

    @Inject(method = "render(Lmchorse/bbs_mod/ui/framework/UIContext;)V", at = @At("HEAD"))
    private void bbslezy$edgeWrap(UIContext context, CallbackInfo ci)
    {
        if (!this.dragging)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int ww = mc.getWindow().getWidth();

        double factor = Math.ceil(ww / (double) context.menu.width);
        int mouseX = context.globalX(context.mouseX);

        final int border = 5;
        final int borderPadding = border + 1;

        if (this.bbslezy$wrapTimer.isTime())
        {
            if (mouseX <= border)
            {
                int targetX = ww - (int) (factor * borderPadding);
                int targetY = (int) mc.mouse.getY();

                Window.moveCursor(targetX, targetY);
                this.initialX += (context.menu.width - borderPadding * 2);
                this.bbslezy$wrapTimer.mark();
            }
            else if (mouseX >= context.menu.width - border)
            {
                int targetX = (int) (factor * borderPadding);
                int targetY = (int) mc.mouse.getY();

                Window.moveCursor(targetX, targetY);
                this.initialX -= (context.menu.width - borderPadding * 2);
                this.bbslezy$wrapTimer.mark();
            }
        }
    }
}
