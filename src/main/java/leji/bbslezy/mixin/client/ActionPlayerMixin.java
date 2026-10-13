package leji.bbslezy.mixin.client;

import leji.bbslezy.server.LezyServerTick;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.Replays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimizes ActionPlayer server tick from O(N^2) to O(N).
 *
 * <p>BBS performs a linear scan over film.replays for every active actor on every tick:
 * {@code (Replay) this.film.replays.get(entry.getKey())}.
 * For films with hundreds/thousands of replays, this causes catastrophic server lag.
 * This mixin caches replays in an O(1) HashMap refreshed on tick start.</p>
 */
@Mixin(value = ActionPlayer.class, remap = false)
public abstract class ActionPlayerMixin
{
    @Shadow
    public Film film;

    @Unique
    private final Map<String, Replay> bbslezy$replaysMap = new HashMap<>();

    @Unique
    private int bbslezy$lastReplayCount = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bbslezy$refreshReplaysCache(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir)
    {
        this.bbslezy$rebuildCacheIfNeeded();
    }

    @Inject(method = "goTo(II)V", at = @At("HEAD"))
    private void bbslezy$refreshCacheOnSeek(int from, int tick, CallbackInfo ci)
    {
        this.bbslezy$rebuildCacheIfNeeded();
    }

    @Unique
    private void bbslezy$rebuildCacheIfNeeded()
    {
        if (this.film == null)
        {
            this.bbslezy$replaysMap.clear();
            this.bbslezy$lastReplayCount = 0;
            return;
        }

        int currentCount = this.film.replays.getList().size();
        if (currentCount != this.bbslezy$lastReplayCount || this.bbslezy$replaysMap.size() != currentCount)
        {
            this.bbslezy$replaysMap.clear();
            for (Replay replay : this.film.replays.getList())
            {
                if (replay != null)
                {
                    this.bbslezy$replaysMap.put(replay.getId(), replay);
                }
            }
            this.bbslezy$lastReplayCount = currentCount;
        }
    }
    /**
     * Short-circuit applyAction() completely if the film has no action clips across all replays.
     * Avoids looping through N replays, looking up actors, and flushing fake player lids.
     */
    @Inject(method = "applyAction", at = @At("HEAD"), cancellable = true)
    private void bbslezy$skipApplyActionIfNoClips(CallbackInfo ci)
    {
        if (this.film == null)
        {
            ci.cancel();
            return;
        }

        boolean hasAnyActions = false;
        for (Replay replay : this.film.replays.getList())
        {
            if (replay != null && !replay.actions.get().isEmpty())
            {
                hasAnyActions = true;
                break;
            }
        }

        if (!hasAnyActions)
        {
            ci.cancel();
        }
    }

    /**
     * Optimize actor.move() inside ActionPlayer.apply().
     * If displacement is near-zero (actor is stationary or moving infinitesimally),
     * skip expensive world block collision checks.
     */
    @Redirect(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V")
    )
    private void bbslezy$optimizedActorMove(net.minecraft.entity.LivingEntity actor, net.minecraft.entity.MovementType type, net.minecraft.util.math.Vec3d movement)
    {
        if (movement.lengthSquared() > LezyServerTick.MOVE_EPSILON_SQ)
        {
            actor.move(type, movement);
        }
    }

    /**
     * Redirects the O(N) linear scan `film.replays.get(key)` inside ActionPlayer.tick and goTo
     * to our O(1) cached map lookup.
     */
    @Redirect(
        method = {"tick", "goTo(II)V"},
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/film/replays/Replays;get(Ljava/lang/String;)Lmchorse/bbs_mod/settings/values/base/BaseValue;")
    )
    private mchorse.bbs_mod.settings.values.base.BaseValue bbslezy$fastReplayLookup(Replays instance, String key)
    {
        Replay cached = this.bbslezy$replaysMap.get(key);
        if (cached != null)
        {
            return cached;
        }
        return instance.get(key);
    }
}
