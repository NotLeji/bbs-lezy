package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;

/**
 * Bridge from the film controller to the smear render. While a replay actor is being rendered,
 * holds its {@link Replay} + {@link IEntity} + the current film time.
 */
public final class SmearReplayState
{
    public static Replay replay;
    public static IEntity entity;
    public static int baseTick;
    public static float transition;
    public static int replayIndex = -1;

    private SmearReplayState()
    {}

    public static void begin(Replay replay, IEntity entity, int baseTick, float transition, int replayIndex)
    {
        SmearReplayState.replay = replay;
        SmearReplayState.entity = entity;
        SmearReplayState.baseTick = baseTick;
        SmearReplayState.transition = transition;
        SmearReplayState.replayIndex = replayIndex;
    }

    public static void end()
    {
        replay = null;
        entity = null;
        baseTick = 0;
        transition = 0F;
        replayIndex = -1;
    }

    /** Current film time in ticks (whole tick + partial) — the present moment to rewind back from. */
    public static float time()
    {
        return baseTick + transition;
    }

    /** True when the bridge currently holds this exact entity (the one being rendered). */
    public static boolean has(IEntity e)
    {
        return replay != null && (entity == null || entity == e);
    }
}
