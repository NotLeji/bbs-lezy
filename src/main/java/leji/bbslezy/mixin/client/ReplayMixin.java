package leji.bbslezy.mixin.client;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Eliminates garbage allocation in Replay.applyActions & applyClientActions.
 *
 * <p>By default, Replay.applyActions calls actions.getClips(tick), which allocates
 * and sorts a new ArrayList on every tick for every replay, even when actions is empty.
 * For 1000 replays, this allocates 20,000+ temporary lists per second.
 * Short-circuiting when actions is empty eliminates this pressure entirely.</p>
 */
@Mixin(value = Replay.class, remap = false)
public abstract class ReplayMixin
{
    @Inject(method = "applyActions", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipEmptyActions(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, int tick, CallbackInfo ci)
    {
        Replay self = (Replay) (Object) this;
        if (self.actions.get().isEmpty())
        {
            ci.cancel();
        }
    }

    @Inject(method = "applyClientActions", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipEmptyClientActions(int tick, IEntity entity, Film film, CallbackInfo ci)
    {
        Replay self = (Replay) (Object) this;
        if (self.actions.get().isEmpty())
        {
            ci.cancel();
        }
    }
}
