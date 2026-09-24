package bbslezy.ui;

import bbslezy.mixin.client.UIFormUndoHandlerMixin;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.ReplayBatchProcessor;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Inserts look-at rotation keyframes at the current timeline playhead tick
 * instead of rewriting the entire channel. Allows stacking multiple look-at
 * keyframes at different ticks across multiple replays.
 *
 * <p>Uses multithreaded calculation across CPU cores for heavy math, and applies
 * keyframe mutations in small batches on the Minecraft main thread with frame yields
 * to prevent UI freezes and eliminate ConcurrentModificationException.</p>
 */
public class LezyLookAt
{
    private static final String[] ROTATION_CHANNELS = {"yaw", "pitch", "headYaw", "bodyYaw"};
    private static final int BATCH_SIZE = 25;
    private static final long BATCH_DELAY_MS = 15L;

    public static class BakedChannel
    {
        public final KeyframeChannel<Double> channel;
        public final float tick;
        public final double angle;

        public BakedChannel(KeyframeChannel<Double> channel, float tick, double angle)
        {
            this.channel = channel;
            this.tick = tick;
            this.angle = angle;
        }

        public void apply()
        {
            this.channel.insertInheriting(this.tick, this.angle);
        }
    }

    public static class BakedReplay
    {
        public final List<BakedChannel> channels = new ArrayList<>();

        public void apply()
        {
            for (BakedChannel bc : this.channels)
            {
                bc.apply();
            }
        }
    }

    public static ReplayBatchProcessor.Error lookAt(List<ReplayBatchProcessor.VisibleReplay> selected, Replay target, float tick, List<String> channels)
    {
        return lookAt(selected, target, tick, channels, null);
    }

    public static ReplayBatchProcessor.Error lookAt(List<ReplayBatchProcessor.VisibleReplay> selected, Replay target, float tick, List<String> channels, UIFilmPanel filmPanel)
    {
        if (target == null)
        {
            return ReplayBatchProcessor.Error.NEED_TARGET;
        }

        if (filmPanel != null && filmPanel.getUndoHandler() != null)
        {
            filmPanel.getUndoHandler().submitUndo(true);
            UIFormUndoHandlerMixin.bbslezy$setBatchLock(true);
        }

        try
        {
            boolean hasAnyRotation = false;

            if (channels != null)
            {
                for (String ch : channels)
                {
                    for (String rot : ROTATION_CHANNELS)
                    {
                        if (rot.equals(ch))
                        {
                            hasAnyRotation = true;
                            break;
                        }
                    }
                }
            }

            boolean allChannels = !hasAnyRotation;

            for (ReplayBatchProcessor.VisibleReplay replay : selected)
            {
                BakedReplay baked = computeForReplay(replay.replay, target, tick, allChannels, channels);

                if (baked != null)
                {
                    baked.apply();
                }
            }
        }
        finally
        {
            UIFormUndoHandlerMixin.bbslezy$setBatchLock(false);

            if (filmPanel != null && filmPanel.getUndoHandler() != null)
            {
                filmPanel.getUndoHandler().submitUndo(true);
                filmPanel.getUndoHandler().getUndoManager().markLastUndoNoMerging();
            }
        }

        return null;
    }

    public static void lookAtAsync(
        List<ReplayBatchProcessor.VisibleReplay> selected,
        Replay target,
        float tick,
        List<String> channels,
        UIBakingProgressOverlayPanel progressPanel,
        UIFilmPanel filmPanel)
    {
        boolean hasAnyRotation = false;

        if (channels != null)
        {
            for (String ch : channels)
            {
                for (String rot : ROTATION_CHANNELS)
                {
                    if (rot.equals(ch))
                    {
                        hasAnyRotation = true;
                        break;
                    }
                }
            }
        }

        boolean allChannels = !hasAnyRotation;
        if (filmPanel != null && filmPanel.getUndoHandler() != null)
        {
            filmPanel.getUndoHandler().submitUndo(true);
            UIFormUndoHandlerMixin.bbslezy$setBatchLock(true);
        }


        /* Step 1: Multithreaded read-only calculation of all look-at angles across all CPU cores */
        ForkJoinPool.commonPool().execute(() ->
        {
            List<BakedReplay> bakedList = selected.parallelStream()
                .map(vr -> computeForReplay(vr.replay, target, tick, allChannels, channels))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            /* Step 2: Post to Minecraft main thread to apply keyframes safely in batches with render yields */
            MinecraftClient.getInstance().execute(() ->
            {
                applyNextBatch(bakedList, 0, BATCH_SIZE, progressPanel, filmPanel);
            });
        });
    }

    private static void applyNextBatch(
        List<BakedReplay> baked,
        int index,
        int batchSize,
        UIBakingProgressOverlayPanel progressPanel,
        UIFilmPanel filmPanel)
    {
        int total = baked.size();
        int end = Math.min(index + batchSize, total);

        for (int i = index; i < end; i++)
        {
            baked.get(i).apply();
        }

        float p = total == 0 ? 1F : (float) end / total;
        int pct = Math.round(p * 100F);
        String status = pct + "% (" + end + " / " + total + ")";

        if (progressPanel != null)
        {
            progressPanel.updateProgress(p, status);
        }

        if (end < total)
        {
            /* Yield on background worker thread to give the Minecraft render thread time to draw */
            ForkJoinPool.commonPool().execute(() ->
            {
                try
                {
                    Thread.sleep(BATCH_DELAY_MS);
                }
                catch (InterruptedException ignored)
                {}

                MinecraftClient.getInstance().execute(() ->
                {
                    applyNextBatch(baked, end, batchSize, progressPanel, filmPanel);
                });
            });
        }
        else
        {
            if (progressPanel != null)
            {
                progressPanel.markFinished();
                progressPanel.close();
            }

            UIFormUndoHandlerMixin.bbslezy$setBatchLock(false);

            if (filmPanel != null)
            {
                if (filmPanel.getUndoHandler() != null)
                {
                    filmPanel.getUndoHandler().submitUndo(true);
                    filmPanel.getUndoHandler().getUndoManager().markLastUndoNoMerging();
                }

                filmPanel.getController().createEntities();
                filmPanel.replayEditor.updateChannelsList();
            }
        }
    }

    public static BakedReplay computeForReplay(
        Replay replay,
        Replay target,
        float tick,
        boolean allChannels,
        List<String> channels)
    {
        if (replay == target)
        {
            return null;
        }

        ReplayKeyframes src = replay.keyframes;
        BakedReplay baked = new BakedReplay();

        for (String id : ROTATION_CHANNELS)
        {
            if (!allChannels && !channels.contains(id))
            {
                continue;
            }

            KeyframeChannel<Double> channel = channel(src, id);

            if (channel == null)
            {
                continue;
            }

            boolean isPitch = id.equals("pitch");
            double angle = compute(tick, src, target.keyframes, isPitch);
            double unwrapped = unwrap(channel, tick, angle, isPitch);

            baked.channels.add(new BakedChannel(channel, tick, unwrapped));
        }

        return baked.channels.isEmpty() ? null : baked;
    }

    private static KeyframeChannel<Double> channel(ReplayKeyframes keyframes, String id)
    {
        return switch (id)
        {
            case "yaw" -> keyframes.yaw;
            case "pitch" -> keyframes.pitch;
            case "headYaw" -> keyframes.headYaw;
            case "bodyYaw" -> keyframes.bodyYaw;
            default -> null;
        };
    }

    private static double compute(float tick, ReplayKeyframes src, ReplayKeyframes target, boolean pitch)
    {
        double sx = src.x.interpolate(tick), sy = src.y.interpolate(tick), sz = src.z.interpolate(tick);
        double tx = target.x.interpolate(tick), ty = target.y.interpolate(tick), tz = target.z.interpolate(tick);
        double dx = tx - sx, dy = ty - sy, dz = tz - sz;

        if (pitch)
        {
            double h = Math.sqrt(dx * dx + dz * dz);

            return -Math.atan2(dy, h) * (180D / Math.PI);
        }

        return Math.atan2(dz, dx) * (180D / Math.PI) - 90D;
    }

    /**
     * Prevent angle wrapping from taking the long way around (e.g. 350° to 10° swinging through 180°).
     * Smooths relative to the keyframe immediately preceding the target tick.
     */
    private static double unwrap(KeyframeChannel<Double> channel, float tick, double angle, boolean isPitch)
    {
        if (isPitch)
        {
            return angle;
        }

        Keyframe<Double> prev = null;

        for (Keyframe<Double> keyframe : channel.getKeyframes())
        {
            if (keyframe.getTick() < tick)
            {
                prev = keyframe;
            }
            else
            {
                break;
            }
        }

        if (prev == null)
        {
            return angle;
        }

        double diff = (angle - prev.getValue()) % 360D;

        if (diff < -180D)
        {
            diff += 360D;
        }
        else if (diff > 180D)
        {
            diff -= 360D;
        }

        return prev.getValue() + diff;
    }
}
