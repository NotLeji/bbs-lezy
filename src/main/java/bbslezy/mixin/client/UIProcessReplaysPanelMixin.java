package bbslezy.mixin.client;

import bbslezy.ui.LezyLookAt;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.ReplayBatchProcessor;
import mchorse.bbs_mod.ui.film.replays.UIProcessReplaysPanel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Inserts look-at rotation keyframes on the current playback playhead tick
 * instead of rewriting the entire channel. Allows stacking multiple look-at
 * keyframes at different ticks across multiple replays.
 */
@Mixin(value = UIProcessReplaysPanel.class, remap = false)
public abstract class UIProcessReplaysPanelMixin
{
    @Final
    @Shadow
    private UIFilmPanel filmPanel;

    @Unique
    private int bbslezy$cursorTick;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$snapshotCursor(CallbackInfo ci)
    {
        this.bbslezy$cursorTick = this.filmPanel != null ? this.filmPanel.getCursor() : 0;
    }

    @Redirect(
        method = "applyNormal",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/film/replays/ReplayBatchProcessor;applyNormal(Ljava/util/List;Ljava/util/List;Lmchorse/bbs_mod/ui/film/replays/ReplayBatchProcessor$Operation;Lmchorse/bbs_mod/ui/film/replays/ReplayBatchProcessor$NormalParams;)Lmchorse/bbs_mod/ui/film/replays/ReplayBatchProcessor$Error;"
        )
    )
    private ReplayBatchProcessor.Error bbslezy$lookAtAtCursor(
        List<ReplayBatchProcessor.VisibleReplay> selected,
        List<String> properties,
        ReplayBatchProcessor.Operation operation,
        ReplayBatchProcessor.NormalParams params)
    {
        if (operation != ReplayBatchProcessor.Operation.LOOK_AT)
        {
            return ReplayBatchProcessor.applyNormal(selected, properties, operation, params);
        }

        return LezyLookAt.lookAt(selected, params.lookAtTarget, this.bbslezy$cursorTick, properties);
    }
}
