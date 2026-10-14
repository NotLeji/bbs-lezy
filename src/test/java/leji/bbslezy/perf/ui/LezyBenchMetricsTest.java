package leji.bbslezy.perf.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LezyBenchMetricsTest
{
    @Test
    public void percentilesAndMax()
    {
        LezyBenchMetrics metrics = new LezyBenchMetrics("test");

        metrics.addFrame(20F);
        metrics.addFrame(10F);
        metrics.addFrame(30F);

        assertEquals(30F, metrics.getMax(), 0.001);
        assertEquals(20F, metrics.getPercentile(50), 0.001);
    }

    @Test
    public void hitchCountsAboveThresholds()
    {
        LezyBenchMetrics metrics = new LezyBenchMetrics("test");

        metrics.addFrame(95F);
        metrics.addFrame(120F);
        metrics.addFrame(250F);

        assertEquals(2, metrics.getCountAbove(100F));
        assertEquals(1, metrics.getCountAbove(200F));
    }

    @Test
    public void summaryContainsPhasesAndHitchCount()
    {
        LezyBenchMetrics alpha = new LezyBenchMetrics("alpha");

        alpha.addFrame(16F);

        LezyBenchMetrics beta = new LezyBenchMetrics("beta");

        beta.addFrame(120F);

        String summary = LezyBenchMetrics.format(List.of(alpha, beta));

        assertTrue(summary.contains("phase=alpha"));
        assertTrue(summary.contains("phase=beta"));
        assertTrue(summary.contains("hitches>100ms="));
    }
}
