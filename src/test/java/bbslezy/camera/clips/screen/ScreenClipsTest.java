package bbslezy.camera.clips.screen;

import bbslezy.utils.keyframes.factories.LensRadiusSettingsKeyframeFactory;
import mchorse.bbs_mod.utils.interps.Interpolations;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenClipsTest
{
    @Test
    void colorClip_properties()
    {
        ColorClip clip = new ColorClip();
        assertNotNull(clip.create());
        assertInstanceOf(ColorClip.class, clip.create());
        assertEquals(14, clip.channels.length);
    }

    @Test
    void vignetteClip_properties()
    {
        VignetteClip clip = new VignetteClip();
        assertNotNull(clip.create());
        assertInstanceOf(VignetteClip.class, clip.create());
        assertEquals(2, clip.channels.length);
    }

    @Test
    void cinematicClip_properties()
    {
        CinematicClip clip = new CinematicClip();
        assertNotNull(clip.create());
        assertInstanceOf(CinematicClip.class, clip.create());
        assertNotNull(clip.vintage, "CinematicClip must have vintage keyframe channel");
        assertNotNull(clip.letterboxHeight, "CinematicClip must have letterbox channel");
        assertNotNull(clip.grainStrength, "CinematicClip must have grain channel");
    }

    @Test
    void lensRadius_interpolation()
    {
        LensRadiusSettings a = new LensRadiusSettings(1F, 1F);
        LensRadiusSettings b = new LensRadiusSettings(3F, 5F);

        LensRadiusSettings result = LensRadiusSettingsKeyframeFactory.INSTANCE.interpolate(
            a, a, b, b, Interpolations.LINEAR, 0.5F
        );

        assertEquals(2F, result.x, 1e-4F);
        assertEquals(3F, result.y, 1e-4F);
    }
}
