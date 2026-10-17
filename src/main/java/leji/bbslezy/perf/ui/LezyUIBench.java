package leji.bbslezy.perf.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_editor.UIModelEditorPanel;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bench headless ber-script: buka dashboard, pindah panel, tandai fase,
 * tulis metrik per fase ke build/uibench/summary.txt, lalu hentikan client.
 * No-op total tanpa -Dbbslezy.uibench (nol overhead runtime normal).
 */
public class LezyUIBench
{
    private static final boolean ENABLED = Boolean.getBoolean("bbslezy.uibench");
    private static final List<LezyBenchMetrics> phases = new ArrayList<>();
    private static LezyBenchMetrics current;
    private static long lastFrame = System.nanoTime();
    private static int waitTicks = 0;
    private static int step = 0;

    public static boolean isEnabled()
    {
        return ENABLED;
    }

    /** Dipanggil dari END_CLIENT_TICK handler di BBSLodClient. */
    public static void tick(MinecraftClient mc)
    {
        if (!ENABLED)
        {
            return;
        }

        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (waitTicks > 0)
        {
            waitTicks--;

            return;
        }

        executeStep(step, mc);

        step++;
    }

    /** Dipanggil dari MinecraftClientMixin render TAIL. */
    public static void onFrameEnd()
    {
        if (!ENABLED || current == null)
        {
            return;
        }

        long now = System.nanoTime();
        float ms = (float) ((now - lastFrame) / 1000000D);

        lastFrame = now;

        if (ms < 10000F)
        {
            current.addFrame(ms);
        }
    }

    private static void executeStep(int stepToRun, MinecraftClient mc)
    {
        switch (stepToRun)
        {
            case 0:
                System.out.println("[BBSLezyUIBench] step 0: launch_wait");
                startPhase("launch_wait");
                waitTicks = 60;
                break;
            case 1:
                System.out.println("[BBSLezyUIBench] step 1: dashboard_open");
                endPhase();
                openDashboard(mc);
                startPhase("dashboard_open");
                waitTicks = 600;
                break;
            case 2:
                System.out.println("[BBSLezyUIBench] step 2: film_panel");
                endPhase();
                showPanelFilm(mc);
                startPhase("film_panel");
                waitTicks = 300;
                break;
            case 3:
                System.out.println("[BBSLezyUIBench] step 3: model_panel");
                endPhase();
                showPanelModel(mc);
                startPhase("model_panel");
                waitTicks = 300;
                break;
            default:
                System.out.println("[BBSLezyUIBench] step " + stepToRun + ": closeAndDump");
                closeAndDump(mc);
                break;
        }
    }

    /** Semua aksi UI dibungkus try/catch agar bench tidak crash bila API berubah. */
    private static void openDashboard(MinecraftClient mc)
    {
        try
        {
            UIScreen.open(BBSModClient.getDashboard());
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void showPanelFilm(MinecraftClient mc)
    {
        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            dashboard.setPanel(dashboard.getPanel(UIFilmPanel.class));
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void showPanelModel(MinecraftClient mc)
    {
        try
        {
            UIDashboard dashboard = BBSModClient.getDashboard();

            dashboard.setPanel(dashboard.getPanel(UIModelEditorPanel.class));
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static void startPhase(String label)
    {
        current = new LezyBenchMetrics(label);
    }

    private static void endPhase()
    {
        if (current != null)
        {
            phases.add(current);
            current = null;
        }
    }

    private static void closeAndDump(MinecraftClient mc)
    {
        endPhase();

        String formatted = LezyBenchMetrics.format(phases);
        System.out.println("[BBSLezyUIBench] summary:\n" + formatted);

        Path target = Path.of("build/uibench/summary.txt");
        Path targetParent = Path.of("../build/uibench/summary.txt");

        for (Path p : List.of(target, targetParent))
        {
            try
            {
                if (p.getParent() != null)
                {
                    Files.createDirectories(p.getParent());
                }

                Files.writeString(p, formatted);
            }
            catch (IOException ignored)
            {
            }
        }

        mc.scheduleStop();
    }
}
