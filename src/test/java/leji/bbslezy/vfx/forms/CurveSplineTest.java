package leji.bbslezy.vfx.forms;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Catmull-Rom spline & sampling pure untuk CurveForm (Port BBSVFX D4).
 */
class CurveSplineTest
{
    @Test
    void sampleIsEndpointValid()
    {
        /* 4-point straight line along X */
        List<Vector3f> positions = Arrays.asList(
            new Vector3f(0F, 0F, 0F),
            new Vector3f(10F, 0F, 0F),
            new Vector3f(20F, 0F, 0F),
            new Vector3f(30F, 0F, 0F)
        );
        float[] widths = new float[]{1F, 1F, 1F, 1F};

        List<CurveSpline.Sample> samples = CurveSpline.build(positions, widths, 10, false);
        assertFalse(samples.isEmpty());

        /* First sample must be at P0 (or P1 in Catmull-Rom open spline start) */
        CurveSpline.Sample first = samples.get(0);
        CurveSpline.Sample last = samples.get(samples.size() - 1);

        assertEquals(0F, first.pos.x, 0.001F);
        assertEquals(30F, last.pos.x, 0.001F);
    }

    @Test
    void analyticPointAndTangent()
    {
        List<Vector3f> positions = Arrays.asList(
            new Vector3f(0F, 0F, 0F),
            new Vector3f(10F, 0F, 0F),
            new Vector3f(20F, 0F, 0F),
            new Vector3f(30F, 0F, 0F)
        );

        /* At span 1, u = 0, point should be P1 = (10, 0, 0) */
        Vector3f pt = CurveSpline.point(positions, 1, 0F, false);
        assertEquals(10F, pt.x, 0.001F);

        /* Tangent should point along +X */
        Vector3f tan = CurveSpline.tangent(positions, 1, 0F, false);
        assertTrue(tan.x > 0F);
        assertEquals(0F, tan.y, 0.001F);
        assertEquals(0F, tan.z, 0.001F);
    }
    @Test
    void trimRestrictsParameterRange()
    {
        List<Vector3f> positions = Arrays.asList(
            new Vector3f(0F, 0F, 0F),
            new Vector3f(10F, 0F, 0F),
            new Vector3f(20F, 0F, 0F),
            new Vector3f(30F, 0F, 0F)
        );
        float[] widths = new float[]{1F, 1F, 1F, 1F};

        List<CurveSpline.Sample> samples = CurveSpline.build(positions, widths, 10, false);
        assertFalse(samples.isEmpty());

        List<CurveSpline.Sample> trimmed = CurveSpline.trim(samples, 0.25F, 0.75F);
        assertFalse(trimmed.isEmpty());

        CurveSpline.Sample first = trimmed.get(0);
        CurveSpline.Sample last = trimmed.get(trimmed.size() - 1);

        /* 0.25 of 30 is ~7.5, 0.75 of 30 is ~22.5 */
        assertTrue(first.pos.x >= 7F && first.pos.x <= 8F);
        assertTrue(last.pos.x >= 22F && last.pos.x <= 23F);
    }
}
