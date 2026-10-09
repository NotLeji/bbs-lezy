package leji.bbslezy.client.screen;

import leji.bbslezy.camera.clips.screen.ColorEffect;
import leji.bbslezy.camera.clips.screen.GrainEffect;
import leji.bbslezy.camera.clips.screen.LetterboxEffect;
import org.junit.jupiter.api.Test;
import leji.bbslezy.camera.clips.screen.HalftoneEffect;
import leji.bbslezy.camera.clips.screen.TintEffect;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order the end-of-frame pass walks the tracks in. The drawing itself needs a GL context, so
 * what is pinned here is the layer list every pass is driven from.
 */
class ScreenEffectLayerOrderTest
{
    @Test
    void everyTrackWithSomethingToDrawGetsOnePassAscending()
    {
        List<ColorEffect> colors = new ArrayList<>();
        ColorEffect high = new ColorEffect();
        high.layer = 3;
        ColorEffect low = new ColorEffect();
        low.layer = 0;
        colors.add(high);
        colors.add(low);

        assertEquals(List.of(0, 3), ScreenEffectRenderer.collectLayers(colors, List.of(), List.of()));
    }

    @Test
    void onePassPerTrackNotPerEffect()
    {
        List<ColorEffect> colors = new ArrayList<>();

        for (int i = 0; i < 4; i++)
        {
            ColorEffect effect = new ColorEffect();
            effect.layer = 2;
            colors.add(effect);
        }

        assertEquals(List.of(2), ScreenEffectRenderer.collectLayers(colors, List.of(), List.of()));
    }

    @Test
    void tracksAreCollectedAcrossEveryEffectFamily()
    {
        ColorEffect grade = new ColorEffect();
        grade.layer = 1;
        GrainEffect grain = new GrainEffect();
        grain.layer = 2;
        LetterboxEffect bars = new LetterboxEffect();
        bars.layer = 0;

        HalftoneEffect halftone = new HalftoneEffect();
        halftone.layer = 3;

        TintEffect tint = new TintEffect();
        tint.layer = 4;

        assertEquals(List.of(0, 1, 2, 3, 4), ScreenEffectRenderer.collectLayers(List.of(grade), List.of(bars), List.of(grain), List.of(halftone), List.of(tint)));
        assertEquals(List.of(0, 1, 2, 3), ScreenEffectRenderer.collectLayers(List.of(grade), List.of(bars), List.of(grain), List.of(halftone)));
        assertEquals(List.of(0, 1, 2), ScreenEffectRenderer.collectLayers(List.of(grade), List.of(bars), List.of(grain)));
    }
    @Test
    void anEmptyFrameDrawsNothing()
    {
        assertTrue(ScreenEffectRenderer.collectLayers(List.of(), List.of(), List.of()).isEmpty());
    }

    @Test
    void layerIsReadBackOffTheEffectItWasRecordedOn()
    {
        ColorEffect effect = new ColorEffect();
        effect.layer = 5;
        effect.renderOrder = 2;

        assertEquals(5, effect.layer());
        assertEquals(2, effect.renderOrder());
    }

    @Test
    void aResetEffectCarriesNoTrackFromTheFrameBefore()
    {
        ColorEffect effect = new ColorEffect();
        effect.layer = 4;
        effect.hasGrade = true;

        effect.reset();

        assertEquals(0, effect.layer());
        assertEquals(false, effect.hasGrade);
    }
}
