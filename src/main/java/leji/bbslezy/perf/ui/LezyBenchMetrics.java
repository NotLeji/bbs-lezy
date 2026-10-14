package leji.bbslezy.perf.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame-time collector untuk UI bench headless: persentil, hitungan hitch,
 * dan ringkasan per-fase yang bisa di-diff lintas run. Rumus persentil di
 * sini dibuat untuk perbandingan lintas-run dalam harness yang sama
 * (baseline vs final) — bukan untuk dicocokkan langsung dengan angka spark.
 */
public class LezyBenchMetrics
{
    private final String label;
    private final List<Float> frames = new ArrayList<>();

    public LezyBenchMetrics(String label)
    {
        this.label = label;
    }

    public void addFrame(float ms)
    {
        if (ms >= 0F && ms < 10000F)
        {
            this.frames.add(ms);
        }
    }

    public float getPercentile(int percentile)
    {
        if (this.frames.isEmpty())
        {
            return 0F;
        }

        List<Float> sorted = new ArrayList<>(this.frames);

        sorted.sort(null);

        int index = Math.min(sorted.size() - 1, (int) Math.round((percentile / 100D) * sorted.size() - 0.5D));

        return sorted.get(index);
    }

    public float getMax()
    {
        return this.getPercentile(100);
    }

    public int getCountAbove(float thresholdMs)
    {
        int count = 0;

        for (float frame : this.frames)
        {
            if (frame > thresholdMs)
            {
                count++;
            }
        }

        return count;
    }

    public float getAvgFps()
    {
        if (this.frames.isEmpty())
        {
            return 0F;
        }

        double totalMs = 0;

        for (float frame : this.frames)
        {
            totalMs += frame;
        }

        return (float) (this.frames.size() / (totalMs / 1000D));
    }

    public String getLabel()
    {
        return this.label;
    }

    /**
     * Satu baris per fase: phase, jumlah frame, persentil, hitch, fps.
     */
    public static String format(List<LezyBenchMetrics> phases)
    {
        StringBuilder sb = new StringBuilder("lezy.uibench summary\n");

        for (LezyBenchMetrics phase : phases)
        {
            sb.append("phase=").append(phase.label);
            sb.append(" frames=").append(phase.frames.size());
            sb.append(String.format(" p50_ms=%.1f", phase.getPercentile(50)));
            sb.append(String.format(" p95_ms=%.1f", phase.getPercentile(95)));
            sb.append(String.format(" p99_ms=%.1f", phase.getPercentile(99)));
            sb.append(String.format(" max_ms=%.1f", phase.getMax()));
            sb.append(" hitches>100ms=").append(phase.getCountAbove(100F));
            sb.append(String.format(" fps=%.1f", phase.getAvgFps()));
            sb.append('\n');
        }

        return sb.toString();
    }
}
