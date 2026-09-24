package bbslezy.ui;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.film.replays.ReplayBatchProcessor;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * Inserts look-at rotation keyframes at the current timeline playhead tick
 * instead of rewriting the entire channel. Allows stacking multiple look-at
 * keyframes at different ticks across multiple replays.
 *
 * <p>Processes in multi-threaded batches with frame delays between batches to
 * prevent UI freezes when baking thousands of replays.</p>
 */
public class LezyLookAt
{
    private static final String[] ROTATION_CHANNELS = {"yaw", "pitch", "headYaw", "bodyYaw"};
    private static final int BATCH_SIZE = 25;
    private static final long BATCH_DELAY_MS = 15L;

    public static ReplayBatchProcessor.Error lookAt(List<ReplayBatchProcessor.VisibleReplay> selected, Replay target, float tick, List<String> channels)
    {
        if (target == null)
        {
            return ReplayBatchProcessor.Error.NEED_TARGET;
        }

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
            applyOne(replay.replay, target, tick, allChannels, channels);
        }

        return null;
    }

    public static void lookAtAsync(
        List<ReplayBatchProcessor.VisibleReplay> selected,
        Replay target,
        float tick,
        List<String> channels,
        UIBakingProgressOverlayPanel progressPanel,
        Runnable onComplete)
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

        Thread bakerThread = new Thread(() ->
        {
            int total = selected.size();

            for (int start = 0; start < total; start += BATCH_SIZE)
            {
                int end = Math.min(start + BATCH_SIZE, total);
                List<ReplayBatchProcessor.VisibleReplay> batch = selected.subList(start, end);

                /* Process this batch across CPU cores in parallel */
                batch.parallelStream().forEach(replay ->
                {
                    applyOne(replay.replay, target, tick, allChannels, channels);
                });

                float p = (float) end / total;
                int pct = Math.round(p * 100F);
                String status = pct + "% (" + end + " / " + total + ")";

                if (progressPanel != null)
                {
                    progressPanel.updateProgress(p, status);
                }

                /* Yield between batches to give render thread time to draw ("gap ruang") */
                try
                {
                    Thread.sleep(BATCH_DELAY_MS);
                }
                catch (InterruptedException ignored)
                {}
            }

            if (onComplete != null)
            {
                MinecraftClient.getInstance().execute(onComplete);
            }
        }, "BBS-Lezy-LookAt-Baker");

        bakerThread.setDaemon(true);
        bakerThread.start();
    }

    public static void applyOne(Replay replay, Replay target, float tick, boolean allChannels, List<String> channels)
    {
        if (replay == target)
        {
            return;
        }

        ReplayKeyframes src = replay.keyframes;

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

            channel.insertInheriting(tick, unwrap(channel, tick, angle, isPitch));
        }
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
