package leji.bbslezy.camera.clips.screen;

import leji.bbslezy.actions.LezyDamageActionClip;
import leji.bbslezy.utils.keyframes.factories.LensRadiusSettingsKeyframeFactory;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.clips.Clip;
import java.util.List;
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
    void letterboxClip_properties()
    {
        LetterboxClip clip = new LetterboxClip();
        assertNotNull(clip.create());
        assertInstanceOf(LetterboxClip.class, clip.create());
        assertEquals(0.48D, clip.height.get(), 1e-4D, "Letterbox should default to 0.48 cinema ratio");
        assertEquals(1.0D, clip.width.get(), 1e-4D);
        assertEquals(8, clip.channels.length);
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
        assertNotNull(clip.grainStrength, "CinematicClip must have grain channel");
        assertEquals(32, clip.channels.length, "Six position channels were added");
        assertNotNull(clip.lensCenterX);
        assertEquals("lens_center_x", clip.lensCenterX.getId());
        assertNotNull(clip.lightLeakCenterY);
        assertEquals("light_leak_center_y", clip.lightLeakCenterY.getId());
    }

    @Test
    void lezyDamageActionClip_properties()
    {
        LezyDamageActionClip clip = new LezyDamageActionClip();
        assertTrue(clip.isClient(), "Damage action clip must be client-enabled for hurt timer & sounds");
        assertEquals(1.0F, clip.damage.get(), 1e-4F, "Default damage should be 1.0");
        assertNotNull(clip.create());
        assertInstanceOf(LezyDamageActionClip.class, clip.create());
    }

    @Test
    void cinematicClip_signedAndUnclampedValuesReachEffect()
    {
        CinematicClip clip = new CinematicClip();

        clip.aberration.insert(0, -1D);
        clip.lensSharpen.insert(0, -1D);
        clip.aberrationDirectional.insert(0, 2D);
        clip.aberrationCenterX.insert(0, 1.5D);
        clip.lensCenterX.insert(0, 0.8D);

        ClipContext context = new ClipContext<CinematicClip, Position>()
        {
            @Override
            public boolean apply(Clip clip, Position position)
            {
                return false;
            }
        };

        context.setup(0, 0, 0F);
        clip.apply(context, new Position(0F, 0F, 0F, 0F, 0F));

        List<ColorEffect> effects = ColorClip.getEffects(context);

        assertEquals(1, effects.size());

        ColorEffect e = effects.get(0);

        /* -1 * 0.25 strength scaling; lens sharpen has no 0.25 scaling */
        assertEquals(-0.25F, e.aberration, 1e-4F);
        assertEquals(-1F, e.lensSharpen, 1e-4F);
        assertEquals(2F, e.aberrationDirectional, 1e-4F, "Directional must not be clamped to 1");
        assertEquals(1.5F, e.aberrationCenterX, 1e-4F, "Centers must accept offscreen values");
        assertEquals(0.8F, e.lensCenterX, 1e-4F);
    }

    @Test
    void cinematicClip_defaultCentersPreserveOldLook()
    {
        CinematicClip clip = new CinematicClip();

        clip.aberration.insert(0, 1D);

        ClipContext context = new ClipContext<CinematicClip, Position>()
        {
            @Override
            public boolean apply(Clip clip, Position position)
            {
                return false;
            }
        };

        context.setup(0, 0, 0F);
        clip.apply(context, new Position(0F, 0F, 0F, 0F, 0F));

        List<ColorEffect> effects = ColorClip.getEffects(context);

        assertEquals(1, effects.size());

        ColorEffect e = effects.get(0);

        /* Defaults are the positions the shader used to hardcode, so old films render the same */
        assertEquals(0.5F, e.lensCenterX, 1e-4F);
        assertEquals(0.5F, e.lensCenterY, 1e-4F);
        assertEquals(0.5F, e.radialBlurCenterX, 1e-4F);
        assertEquals(0.5F, e.radialBlurCenterY, 1e-4F);
        assertEquals(0.0F, e.lightLeakCenterX, 1e-4F);
        assertEquals(0.4F, e.lightLeakCenterY, 1e-4F);
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
