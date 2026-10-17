package leji.bbslezy.perf;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
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
    private static Field runningField;

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

            if (client.getServer() != null)
            {
                try
                {
                    client.getServer().getOverworld().getGameRules().get(BBSMod.BBS_EDITING_RULE).set(true, client.getServer());
                }
                catch (Throwable ignored)
                {}
            }

            Film film = BBSMod.getFilms().load(config.filmId());

            if (film != null)
            {
                film.setId(config.filmId());
                Films.playFilm(film, config.withCamera());
                System.out.println("BBSLezy[bench] film loaded successfully: replays=" + film.replays.getList().size());
            }
            else
            {
                System.out.println("BBSLezy[bench] BBSMod.getFilms().load returned null, fallback to Films.playFilm(id)");
                Films.playFilm(config.filmId(), config.withCamera());
            }

            return;
        }

        ticks++;
        keepRunning(client);

        if (!BBSModClient.getFilms().has(config.filmId()))
        {
            Film film = BBSMod.getFilms().load(config.filmId());

            if (film != null)
            {
                film.setId(config.filmId());
                Films.playFilm(film, config.withCamera());
            }
        }

        if (ticks <= config.warmupTicks())
        {
            if (ticks % 100 == 0)
            {
                System.out.println("BBSLezy[bench] warmup tick=" + ticks + "/" + config.warmupTicks() + " playing=" + BBSModClient.getFilms().has(config.filmId()));
            }

            return;
        }

        int fps = client.getCurrentFps();

        sum += fps;
        count++;
        min = Math.min(min, fps);
        max = Math.max(max, fps);

        if (ticks % 20 == 0)
        {
            System.out.println("BBSLezy[bench] fps=" + fps + " tick=" + ticks + " playing=" + BBSModClient.getFilms().has(config.filmId()));
        }

        if (ticks >= config.warmupTicks() + config.sampleTicks())
        {
            double avg = count == 0 ? 0 : (double) sum / count;

            System.out.println("BBSLezy[bench] summary film=" + config.filmId() + " ticks=" + ticks + " avg=" + String.format("%.1f", avg) + " min=" + (count == 0 ? 0 : min) + " max=" + max);
            client.scheduleStop();
        }
    }

    /**
     * Prevents test-runner harnesses from prematurely stopping the client
     * before the benchmark duration has elapsed.
     */
    private static void keepRunning(MinecraftClient client)
    {
        try
        {
            if (runningField == null)
            {
                for (String name : new String[] {"running", "field_1698"})
                {
                    try
                    {
                        runningField = MinecraftClient.class.getDeclaredField(name);
                        runningField.setAccessible(true);
                        break;
                    }
                    catch (NoSuchFieldException ignored)
                    {}
                }
            }

            if (runningField != null)
            {
                runningField.setBoolean(client, true);
            }
        }
        catch (Exception ignored)
        {}
    }

    /**
     * Copies the bench film into the world's film folder when missing.
     */
    private static void stageFilm(String filmId)
    {
        if ("vfx-mixed-bench".equals(filmId))
        {
            createMixedVfxFilm(filmId);
            return;
        }

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

    private static void createMixedVfxFilm(String filmId)
    {
        try
        {
            File dest = BBSMod.getFilms().getFile(filmId);

            if (dest.exists())
            {
                System.out.println("BBSLezy[bench] vfx-mixed-bench film file present");
                return;
            }

            Film film = new Film();
            film.setId(filmId);

            for (int i = 0; i < 200; i++)
            {
                mchorse.bbs_mod.film.replays.Replay r = film.replays.addReplay();
                double rx = ((i % 20) - 10) * 3.5;
                double rz = ((i / 20) - 5) * 3.5;
                r.keyframes.x.insert(0F, rx);
                r.keyframes.y.insert(0F, 64.0);
                r.keyframes.z.insert(0F, rz);
            }

            mchorse.bbs_mod.film.replays.Replay expReplay = film.replays.addReplay();
            expReplay.category.set("vfx");
            expReplay.keyframes.x.insert(0F, 0.0);
            expReplay.keyframes.y.insert(0F, 64.0);
            expReplay.keyframes.z.insert(0F, 0.0);
            leji.bbslezy.vfx.forms.ExplosionForm expForm = new leji.bbslezy.vfx.forms.ExplosionForm();
            expForm.fillTestStructure(10);
            expForm.destruction.set(0.6F);
            expForm.fireCount.set(20);
            expForm.fireDuration.set(10F);
            expReplay.form.set(expForm);

            mchorse.bbs_mod.film.replays.Replay windReplay = film.replays.addReplay();
            windReplay.category.set("vfx");
            windReplay.keyframes.x.insert(0F, 8.0);
            windReplay.keyframes.y.insert(0F, 64.0);
            windReplay.keyframes.z.insert(0F, 8.0);
            leji.bbslezy.vfx.forms.LezyWindForm windForm = new leji.bbslezy.vfx.forms.LezyWindForm();
            windForm.windType.set(2);
            windForm.strength.set(8F);
            windForm.dust.set(0.8F);
            windForm.streaks.set(1F);
            windForm.leaves.set(1F);
            windForm.progress.set(0.5F);
            windReplay.form.set(windForm);

            mchorse.bbs_mod.film.replays.Replay domeReplay = film.replays.addReplay();
            domeReplay.category.set("vfx");
            domeReplay.keyframes.x.insert(0F, -8.0);
            domeReplay.keyframes.y.insert(0F, 64.0);
            domeReplay.keyframes.z.insert(0F, -8.0);
            leji.bbslezy.vfx.forms.DomeForm domeForm = new leji.bbslezy.vfx.forms.DomeForm();
            domeForm.progress.set(0.4F);
            domeForm.maxRadius.set(24F);
            domeForm.opacity.set(0.9F);
            domeReplay.form.set(domeForm);

            leji.bbslezy.vfx.camera.ImpactClip impactClip = new leji.bbslezy.vfx.camera.ImpactClip();
            impactClip.tick.set(0);
            impactClip.duration.set(2400);
            impactClip.zoomBlur.set(0.25F);
            impactClip.zoomLines.set(0.35F);
            impactClip.shapes.set(0.5F);
            film.camera.add(impactClip);

            dest.getParentFile().mkdirs();
            BBSMod.getFilms().save(filmId, (mchorse.bbs_mod.data.types.MapType) film.toData());
            System.out.println("BBSLezy[bench] created and saved vfx-mixed-bench film successfully");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
