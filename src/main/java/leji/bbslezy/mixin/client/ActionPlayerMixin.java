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
     * Near-zero displacement skips world block collision, as does a
     * pure gravity-probe step from a stationary grounded actor
     * (setPosition + setOnGround in the body still apply).
     */
    @Redirect(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V")
    )
    private void bbslezy$optimizedActorMove(net.minecraft.entity.LivingEntity actor, net.minecraft.entity.MovementType type, net.minecraft.util.math.Vec3d movement)
    {
        if (LezyServerTick.shouldCollide(movement.x, movement.y, movement.z))
        {
            actor.move(type, movement);
        }
    }

    /**
     * Skip equipment writes when both sides are empty.
     * Crowd films (villagers) carry no gear: 6 equipStack writes
     * per actor per tick become 6 isEmpty checks.
     */
    @Redirect(
        method = "apply",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V")
    )
    private void bbslezy$equipOnlyOnChange(net.minecraft.entity.LivingEntity actor, net.minecraft.entity.EquipmentSlot slot, net.minecraft.item.ItemStack stack)
    {
        try
        {
            if (LezyServerTick.skipEquipWhenBothEmpty(actor.getEquippedStack(slot).isEmpty(), stack.isEmpty()))
            {
                return;
            }
        }
        catch (Throwable ignored)
        {
        }

        try
        {
            actor.equipStack(slot, stack);
        }
        catch (Throwable ignored)
        {
        }
    }

    /**
     * Redirects the O(N) linear scan `film.replays.get(key)` inside ActionPlayer tick, seeks
     * and lost-actor scans to our O(1) cached map lookup.
     */
    @Redirect(
        method = {"tick", "goTo(II)V", "reviveLostActors"},
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
