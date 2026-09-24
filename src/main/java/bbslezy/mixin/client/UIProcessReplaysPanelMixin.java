package bbslezy.mixin.client;

import bbslezy.ui.LezyLookAt;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.ReplayBatchProcessor;
import mchorse.bbs_mod.ui.film.replays.UIProcessReplaysPanel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
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
    @Mutable
    @Final
    @Shadow
    private UIFilmPanel filmPanel;

    @Unique
    private int bbslezy$cursorTick;

    /**
     * Assign filmPanel immediately after super() before field initializers run,
     * preventing NPE in UINormalProcessView when PROCESS_STATE.operation == LOOK_AT.
     */
    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/framework/elements/overlay/UIConfirmOverlayPanel;<init>(Lmchorse/bbs_mod/l10n/keys/IKey;Lmchorse/bbs_mod/l10n/keys/IKey;Ljava/util/function/Consumer;)V",
            shift = At.Shift.AFTER
        )
    )
    private void bbslezy$initEarly(UIFilmPanel filmPanel, List replays, CallbackInfo ci)
    {
        this.filmPanel = filmPanel;
        this.bbslezy$cursorTick = filmPanel != null ? filmPanel.getCursor() : 0;
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

        int tick = this.filmPanel != null ? this.filmPanel.getCursor() : this.bbslezy$cursorTick;

        return LezyLookAt.lookAt(selected, params.lookAtTarget, tick, properties);
    }
}
