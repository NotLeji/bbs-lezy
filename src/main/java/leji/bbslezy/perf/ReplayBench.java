package leji.bbslezy.perf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Films;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

/**
 * Headless replay playback benchmark, driven by {@code -Dbbslezy.bench}.
 *
 * <p>Spec format: {@code <filmId>,<warmupTicks>,<sampleTicks>,<withCamera>}.
 * Zero cost when the property is unset: {@link #parse} returns null and
 * the tick handler returns after the started flag.</p>
 */
public class ReplayBench
{
    public record Config(String filmId, int warmupTicks, int sampleTicks, boolean withCamera)
    {}

    private static boolean started;
    private static int ticks;
    private static long sum;
    private static int count;
    private static int min = Integer.MAX_VALUE;
    private static int max;

    /**
     * Parses a bench spec, returning null when it is missing or malformed.
     */
    public static Config parse(String spec)
    {
        if (spec == null || spec.isEmpty())
        {
            return null;
        }

        String[] parts = spec.split(",", -1);

        if (parts.length != 4)
        {
            return null;
        }

        String filmId = parts[0].trim();

        if (filmId.isEmpty())
        {
            return null;
        }

        int warmup;
        int sample;

        try
        {
            warmup = Integer.parseInt(parts[1].trim());
            sample = Integer.parseInt(parts[2].trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        if (warmup < 0 || sample < 0)
        {
            return null;
        }

        String camera = parts[3].trim();

        if (camera.equals("true"))
        {
            return new Config(filmId, warmup, sample, true);
        }
        else if (camera.equals("false"))
        {
            return new Config(filmId, warmup, sample, false);
        }

        return null;
    }

    /**
     * Tick handler for the bench run. One-shot: if the world reloads
     * mid-bench, samples are not reused.
     */
    public static void onClientTick(MinecraftClient client)
    {
        Config config = parse(System.getProperty("bbslezy.bench"));

        if (config == null)
        {
            return;
        }

        if (client.world == null)
        {
            return;
        }

        if (!started)
        {
            started = true;
            ticks = 0;
            sum = 0;
            count = 0;
            min = Integer.MAX_VALUE;
            max = 0;

            stageFilm(config.filmId());
            Films.playFilm(config.filmId(), config.withCamera());

            return;
        }

        ticks++;

        if (ticks <= config.warmupTicks())
        {
            return;
        }

        int fps = client.getCurrentFps();

        sum += fps;
        count++;
        min = Math.min(min, fps);
        max = Math.max(max, fps);

        System.out.println("BBSLezy[bench] fps=" + fps + " tick=" + ticks + " playing=" + BBSModClient.getFilms().has(config.filmId()));

        if (ticks >= config.warmupTicks() + config.sampleTicks())
        {
            double avg = count == 0 ? 0 : (double) sum / count;

            System.out.println("BBSLezy[bench] summary film=" + config.filmId() + " ticks=" + ticks + " avg=" + String.format("%.1f", avg) + " min=" + (count == 0 ? 0 : min) + " max=" + max);
            client.scheduleStop();
        }
    }

    /**
     * Copies the bench film into the world's film folder when missing.
     */
    private static void stageFilm(String filmId)
    {
        try
        {
            File dest = BBSMod.getFilms().getFile(filmId);

            if (dest.exists())
            {
                System.out.println("BBSLezy[bench] film file present");

                return;
            }

            Path source = FabricLoader.getInstance().getGameDir().resolve("bench-films").resolve(filmId + ".dat");

            dest.getParentFile().mkdirs();
            Files.copy(source, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("BBSLezy[bench] staged film from bench-films");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
