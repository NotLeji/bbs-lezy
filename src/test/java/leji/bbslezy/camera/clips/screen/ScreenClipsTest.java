package leji.bbslezy.camera.clips.screen;

import leji.bbslezy.actions.LezyDamageActionClip;
import leji.bbslezy.utils.keyframes.factories.LensRadiusSettingsKeyframeFactory;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.clips.Clip;
import java.util.List;
import mchorse.bbs_mod.utils.interps.Interpolations;
import org.junit.jupiter.api.Test;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
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
        assertEquals(26, clip.channels.length, "Seven aberration channels removed");
        assertEquals("chromatic_aberration", clip.chromaticAberration.getId());
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

        clip.chromaticAberration.insert(0, -1D);
        clip.lensSharpen.insert(0, -1D);
        clip.chromaticAberrationCenterX.insert(0, 1.5D);
        clip.lensCenterX.insert(0, 0.8D);

        clip.pixelation.insert(0, 2D);
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

        /* -1 * 0.05 strength scaling; lens sharpen scaled by 0.20 */
        assertEquals(-0.05F, e.chromaticAberration, 1e-4F);
        assertEquals(-0.20F, e.lensSharpen, 1e-4F);
        assertEquals(1.5F, e.chromaticAberrationCenterX, 1e-4F, "Centers must accept offscreen values");
        assertEquals(0.8F, e.lensCenterX, 1e-4F);
        assertEquals(2F, e.pixelation, 1e-4F);
    }

    @Test
    void cinematicClip_defaultCentersPreserveOldLook()
    {
        CinematicClip clip = new CinematicClip();

        clip.chromaticAberration.insert(0, 1D);

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
    void cinematicClip_legacyAberrationMigration()
    {
        KeyframeFactories.setup();
        CinematicClip legacySource = new CinematicClip();
        legacySource.chromaticAberration.insert(10F, 0.75D);
        legacySource.chromaticAberrationCenterX.insert(15F, 0.25D);

        mchorse.bbs_mod.data.types.MapType data = (mchorse.bbs_mod.data.types.MapType) legacySource.toData();
        data.put("aberration", data.get("chromatic_aberration"));
        data.remove("chromatic_aberration");
        data.put("aberration_center_x", data.get("chromatic_aberration_center_x"));
        data.remove("chromatic_aberration_center_x");

        CinematicClip restored = new CinematicClip();
        restored.fromData(data);

        assertEquals(1, restored.chromaticAberration.getKeyframes().size());
        assertEquals(0.75D, (Double) restored.chromaticAberration.get(0).getValue(), 1e-4);
        assertEquals(1, restored.chromaticAberrationCenterX.getKeyframes().size());
        assertEquals(0.25D, (Double) restored.chromaticAberrationCenterX.get(0).getValue(), 1e-4);
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
