package leji.bbslezy.camera.clips.screen;

import leji.bbslezy.actions.LezyDamageActionClip;
import leji.bbslezy.utils.keyframes.factories.LensRadiusSettingsKeyframeFactory;
import leji.bbslezy.camera.clips.modifiers.ProceduralShakeClip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.clips.Clip;
import java.util.List;
import mchorse.bbs_mod.utils.interps.Interpolations;
import org.junit.jupiter.api.Test;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import static org.junit.jupiter.api.Assertions.*;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.ListType;

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
        assertEquals(28, clip.channels.length, "26 base + 2 motion channels");
        assertEquals("chromatic_aberration", clip.chromaticAberration.getId());
        assertNotNull(clip.lensCenterX);
        assertEquals("lens_center_x", clip.lensCenterX.getId());
        assertNotNull(clip.lightLeakCenterY);
        assertEquals("light_leak_center_y", clip.lightLeakCenterY.getId());
        assertNotNull(clip.motionBlur);
        assertEquals("motion_blur", clip.motionBlur.getId());
        assertNotNull(clip.motionTrail);
        assertEquals("motion_trail", clip.motionTrail.getId());
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
        clip.motionBlur.insert(0, 5D);
        clip.motionTrail.insert(0, 3D);
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
        assertEquals(0.50F, e.motionBlur, 1e-4F);
        assertEquals(0.30F, e.motionTrail, 1e-4F);
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
    @Test
    void halftoneClip_properties()
    {
        HalftoneClip clip = new HalftoneClip();
        assertNotNull(clip.create());
        assertInstanceOf(HalftoneClip.class, clip.create());
        assertEquals(4, clip.channels.length);
        assertEquals(HalftoneEffect.MODE_BW, clip.mode.get());
        assertEquals(0xff000000, clip.inkColor.get());
        assertEquals("cells", clip.cells.getId());
        assertEquals("highlight_threshold", clip.highlightThreshold.getId());
        assertEquals("dot_softness", clip.dotSoftness.getId());
        assertEquals("angle", clip.angle.getId());
    }

    @Test
    void halftoneClip_applyClipDefaultsAndKeyframes()
    {
        HalftoneClip clip = new HalftoneClip();
        ClipContext context = new ClipContext<HalftoneClip, Position>()
        {
            @Override
            public boolean apply(Clip clip, Position position)
            {
                return false;
            }
        };
        context.setup(0, 0, 0F);
        Position position = new Position();

        clip.apply(context, position);
        List<HalftoneEffect> effects = HalftoneClip.getEffects(context);
        assertEquals(1, effects.size());

        HalftoneEffect effect = effects.get(0);
        assertEquals(HalftoneEffect.MODE_BW, effect.mode);
        assertEquals(0xff000000, effect.inkColor);
        assertEquals(130F, effect.cells, 1e-4F);
        assertEquals(0.85F, effect.highlightThreshold, 1e-4F);
        assertEquals(0.05F, effect.dotSoftness, 1e-4F);
        assertEquals(45F, effect.angle, 1e-4F);

        // Test custom keyframed values
        clip.cells.insert(0, 200.0);
        clip.angle.insert(0, 30.0);
        clip.mode.set(HalftoneEffect.MODE_COLOR);
        effects.clear();
        clip.apply(context, position);
        assertEquals(1, effects.size());
        assertEquals(HalftoneEffect.MODE_COLOR, effect.mode);
        assertEquals(200F, effect.cells, 1e-4F);
        assertEquals(30F, effect.angle, 1e-4F);
    }

    @Test
    void letterboxClip_aspectRatioPresetMath()
    {
        LetterboxClip clip = new LetterboxClip();
        float canvasRatio = 16F / 9F; // ~1.7778

        // SCOPE 2.39: wider than canvas -> horizontal bars
        clip.aspectPreset.set(AspectRatioPreset.SCOPE_239.ordinal());
        clip.applyAspectRatioPreset(canvasRatio);
        double expectedHeight = 2D * (1D - (double) canvasRatio / 2.39D);
        assertEquals(expectedHeight, clip.height.get(), 1e-3D);
        assertEquals(1.0D, clip.width.get(), 1e-3D);

        // SHORTS 9:16 (0.5625): narrower than canvas -> pillarbox (height = 0, width < 1)
        clip.aspectPreset.set(AspectRatioPreset.SHORTS_916.ordinal());
        clip.applyAspectRatioPreset(canvasRatio);
        assertEquals(0.0D, clip.height.get(), 1e-3D);
        double expectedWidth = (9D / 16D) / (double) canvasRatio;
        assertEquals(expectedWidth, clip.width.get(), 1e-3D);
        // Pillarbox effect is added to context even when height is 0
        ClipContext context = dummyContext(0, 0F);
        LetterboxClip.getEffects(context).clear();
        Position position = new Position();
        clip.apply(context, position);
        assertEquals(1, LetterboxClip.getEffects(context).size());
        assertEquals(0F, LetterboxClip.getEffects(context).get(0).size);
        assertEquals((float) expectedWidth, LetterboxClip.getEffects(context).get(0).width, 1e-3F);

        // CUSTOM: leaves manual height/width completely untouched
        clip.aspectPreset.set(AspectRatioPreset.CUSTOM.ordinal());
        clip.height.set(0.35D);
        clip.width.set(0.80D);
        clip.applyAspectRatioPreset(canvasRatio);
        assertEquals(0.35D, clip.height.get(), 1e-3D, "Custom preset must leave height untouched");
        assertEquals(0.80D, clip.width.get(), 1e-3D, "Custom preset must leave width untouched");
    }

    @Test
    void letterboxClip_channelGroupAndSmoothness()
    {
        LetterboxClip clip = new LetterboxClip();

        // Ensure static properties are registered at root without keyframe channel collisions
        assertInstanceOf(ValueDouble.class, clip.get("height"), "height must be ValueDouble at root");
        assertInstanceOf(ValueDouble.class, clip.get("width"), "width must be ValueDouble at root");
        assertInstanceOf(ValueDouble.class, clip.get("smoothness"), "smoothness must be ValueDouble at root");
        assertInstanceOf(ValueColor.class, clip.get("color"), "color must be ValueColor at root");
        assertInstanceOf(ValueGroup.class, clip.get("channels"), "channels must be ValueGroup sub-group");

        // Keyframe channels live inside channelsGroup with their expected channel IDs
        assertInstanceOf(KeyframeChannel.class, clip.channelsGroup.get("height"));
        assertInstanceOf(KeyframeChannel.class, clip.channelsGroup.get("smoothness"));

        // Smoothness can be adjusted and propagates to LetterboxEffect
        clip.smoothness.set(0.65D);
        assertEquals(0.65D, clip.smoothness.get(), 1e-4D);

        ClipContext context = dummyContext(0, 0F);
        LetterboxClip.getEffects(context).clear();
        Position position = new Position();
        clip.apply(context, position);

        assertEquals(1, LetterboxClip.getEffects(context).size());
        assertEquals(0.65F, LetterboxClip.getEffects(context).get(0).smoothness, 1e-4F);

        // Backward compatibility: fromData loading root-level keyframe lists
        MapType data = new MapType();
        data.putDouble("smoothness", 0.35D);
        data.putDouble("height", 0.50D);
        ListType kfList = new ListType();
        data.put("height", kfList); // old format had channel at root

        LetterboxClip loaded = new LetterboxClip();
        loaded.fromData(data);
        assertEquals(0.35D, loaded.smoothness.get(), 1e-4D);
    }


    @Test
    void impactClip_punchInFov()
    {
        ImpactClip clip = new ImpactClip();
        clip.duration.set(30);
        clip.punchInEnabled.set(true);
        clip.punchInFov.set(20F);
        clip.punchInDuration.set(10);
        clip.freezeEnabled.set(false);
        clip.shakeEnabled.set(false);
        clip.flashEnabled.set(false);

        ClipContext context = dummyContext(0, 0F);
        Position position = new Position();
        position.angle.fov = 70F;

        // at t=0, punch-in is maximum (fov drops by 20 -> 50)
        context.setup(0, 0, 0F);
        clip.apply(context, position);
        assertEquals(50F, position.angle.fov, 1e-3F);

        // at t=10 (end of punchInDuration), decay is (1-1)^2 = 0 -> fov unaffected
        position.angle.fov = 70F;
        context.setup(10, 10, 0F);
        assertEquals(70F, position.angle.fov, 1e-3F);
    }

    @Test
    void proceduralShakeClip_presetsAndDeterminism()
    {
        ProceduralShakeClip clip = new ProceduralShakeClip();
        clip.applyPreset(ProceduralShakeClip.PRESET_EXPLOSION);

        assertEquals(3.0F, clip.frequency.get());
        assertEquals(2.0F, clip.intensity.get());
        assertEquals(0.25F, clip.posX.get());

        clip.duration.set(30);
        ClipContext context = dummyContext(5, 0.5F);
        context.count = 1;
        Position pos1 = new Position();
        clip.apply(context, pos1);

        Position pos2 = new Position();
        clip.apply(context, pos2);

        assertEquals(pos1.angle.yaw, pos2.angle.yaw, 1e-6F, "Shake must be deterministic");
        assertEquals(pos1.angle.pitch, pos2.angle.pitch, 1e-6F, "Shake must be deterministic");
    }
    @Test
    void proceduralShakeClip_preventsRunawayDriftWithoutBaseClip()
    {
        ProceduralShakeClip clip = new ProceduralShakeClip();
        clip.duration.set(30);

        // When running past base clips (context.count == 0, no clip underneath), position is untouched
        ClipContext emptyContext = dummyContext(5, 0.5F);
        emptyContext.count = 0;

        Position pos = new Position();
        float initialYaw = pos.angle.yaw;
        float initialPitch = pos.angle.pitch;

        clip.apply(emptyContext, pos);

        assertEquals(initialYaw, pos.angle.yaw, 1e-6F, "Shake must not mutate orientation without a base camera clip");
        assertEquals(initialPitch, pos.angle.pitch, 1e-6F, "Shake must not mutate orientation without a base camera clip");
    }

    @SuppressWarnings("unchecked")
    private static ClipContext dummyContext(int ticks, float transition)
    {
        ClipContext context = new ClipContext()
        {
            @Override
            public boolean apply(Clip clip, Object position)
            {
                return false;
            }
        };
        context.setup(ticks, ticks, transition);
        return context;
    }

}
