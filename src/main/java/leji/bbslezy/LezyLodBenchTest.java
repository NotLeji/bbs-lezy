package leji.bbslezy;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.ActionManager;
import mchorse.bbs_mod.actions.ActionPlayer;
import mchorse.bbs_mod.actions.PlayerType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds walking-actor films at several scales and plays each through the real
 * server {@link ActionPlayer} path, printing per-tick cost per scale.
 *
 * <p>Scale via {@code -Dlezy.lodBench.scales="64x100,256x100,1024x40"}.
 * Defaults are CI-sized; pass bigger N locally.</p>
 *
 * <p>Measures the SERVER half (actor tick + apply + actions). LOD only cuts
 * client render, so these numbers must not move with render_limit — that is
 * the point: it separates tick cost from frame cost. Prints incrementally so
 * a harness timeout still leaves partial data in the log.</p>
 */
public class LezyLodBenchTest implements FabricGameTest
{
    private static final String EMPTY = FabricGameTest.EMPTY_STRUCTURE;
    private static final String FILM_ID = "lod_bench";
    private static final int COLS = 64;
    private static final double SPACING = 1.0D;

    @GameTest(templateName = EMPTY, duration = 3600, tickLimit = 4000)
    public void walkingCast(TestContext context)
    {
        String[] parts = System.getProperty("lezy.lodBench.scales", "64x100,256x100,1024x40").split(",");
        ServerWorld world = context.getWorld();
        ActionManager actions = BBSMod.getActions();
        BlockPos origin = context.getAbsolutePos(BlockPos.ORIGIN);

        AtomicInteger scaleIndex = new AtomicInteger();
        AtomicReference<Scale> current = new AtomicReference<>(parse(parts[0]));
        AtomicReference<ActionPlayer> holder = new AtomicReference<>();
        AtomicLong totalNs = new AtomicLong();
        AtomicInteger ran = new AtomicInteger();

        context.runAtTick(1, () ->
        {
            holder.set(startScale(actions, world, origin, current.get()));
        });

        context.runAtEveryTick(() ->
        {
            ActionPlayer player = holder.get();
            Scale scale = current.get();

            if (player == null)
            {
                return;
            }

            if (ran.get() < scale.ticks)
            {
                long start = System.nanoTime();
                boolean finished = player.tick();
                totalNs.addAndGet(System.nanoTime() - start);

                if (finished || ran.incrementAndGet() >= scale.ticks)
                {
                    finishScale(actions, scale, totalNs.get(), ran.get());
                }
                else
                {
                    return;
                }
            }

            int next = scaleIndex.incrementAndGet();

            if (next >= parts.length)
            {
                context.complete();

                return;
            }

            current.set(parse(parts[next]));
            ran.set(0);
            totalNs.set(0);
            holder.set(startScale(actions, world, origin, current.get()));
        });
    }

    private static void finishScale(ActionManager actions, Scale scale, long totalNs, int ticks)
    {
        int n = Math.max(ticks, 1);
        long ms = totalNs / 1_000_000L;

        System.out.println("LOD_BENCH replays=" + scale.replays + " ticks=" + n
            + " totalMs=" + ms + " avgTickMs=" + String.format("%.3f", ms / (double) n));
        actions.stop(FILM_ID);
    }

    private static ActionPlayer startScale(ActionManager actions, ServerWorld world, BlockPos origin, Scale scale)
    {
        actions.stop(FILM_ID);

        Film film = new Film();
        film.setId(FILM_ID);

        for (int i = 0; i < scale.replays; i++)
        {
            Replay replay = film.replays.addReplay();
            replay.actor.set(true);

            double baseX = origin.getX() + (i % COLS) * SPACING;
            double baseZ = origin.getZ() + (i / COLS) * SPACING;
            double baseY = origin.getY() + 1.0D;

            for (int t = 0; t <= 200; t += 10)
            {
                replay.keyframes.x.insert(t, baseX + t * 0.1D);
                replay.keyframes.y.insert(t, baseY);
                replay.keyframes.z.insert(t, baseZ);
            }
        }

        ActionPlayer player = actions.play(null, world, film, 0, PlayerType.FILM_EDITOR);

        System.out.println("LOD_BENCH_START replays=" + scale.replays + " filmReplays=" + film.replays.getList().size()
            + " filmDuration=" + film.calculateDuration());

        return player;
    }

    private static Scale parse(String part)
    {
        String[] split = part.trim().split("x");

        return new Scale(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    private static class Scale
    {
        final int replays;
        final int ticks;

        Scale(int replays, int ticks)
        {
            this.replays = replays;
            this.ticks = ticks;
        }
    }
}
