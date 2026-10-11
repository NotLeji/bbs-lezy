package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.LezyVfxRenderCompat;
import leji.bbslezy.vfx.client.SmearReplayState;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes the current replay actor + film time into {@link SmearReplayState} for the whole duration
 * of that actor's render, so destruction scrubbing and effects know the active replay and timeline duration.
 */
@Mixin(value = BaseFilmController.class, remap = false)
public abstract class SmearReplayBridgeMixin
{
    @Shadow
    public abstract int getTick();

    @Shadow
    protected abstract float getTransition(IEntity entity, float transition);

    @Shadow
    @Final
    public Film film;

    @Inject(method = "renderEntity(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;Lmchorse/bbs_mod/film/replays/Replay;Lmchorse/bbs_mod/forms/entities/IEntity;)V", at = @At("HEAD"))
    private void bbslezy$smearBridgeBegin(WorldRenderContext context, Replay replay, IEntity entity, CallbackInfo ci)
    {
        int baseTick = replay.getTick(this.getTick());
        float delta = this.getTransition(entity, LezyVfxRenderCompat.tickDelta(context));
        int index = this.film != null ? this.film.replays.getList().indexOf(replay) : -1;

        SmearReplayState.begin(replay, entity, baseTick, delta, index);
    }

    @Inject(method = "renderEntity(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;Lmchorse/bbs_mod/film/replays/Replay;Lmchorse/bbs_mod/forms/entities/IEntity;)V", at = @At("RETURN"))
    private void bbslezy$smearBridgeEnd(WorldRenderContext context, Replay replay, IEntity entity, CallbackInfo ci)
    {
        SmearReplayState.end();
    }
}
