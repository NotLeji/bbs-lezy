package leji.bbslezy.vfx.camera;

import org.junit.jupiter.api.AfterEach;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for ported BBSVFX ImpactClip and BbsVfxImpactState. */
class ImpactClipTest
{
    @AfterEach
    void cleanup()
    {
        BbsVfxImpactState.reset();
    }

    @Test
    void stateInitialAndReset()
    {
        BbsVfxImpactState.reset();
        assertFalse(BbsVfxImpactState.active);
        assertFalse(BbsVfxImpactState.hasEffect());

        BbsVfxImpactState.active = true;
        BbsVfxImpactState.silhouette = 0.5F;
        assertTrue(BbsVfxImpactState.hasEffect());

        BbsVfxImpactState.reset();
        assertFalse(BbsVfxImpactState.active);
        assertFalse(BbsVfxImpactState.hasEffect());
        assertEquals(0F, BbsVfxImpactState.silhouette);
    }

    @Test
    void clipInitializationAndChannels()
    {
        ImpactClip clip = new ImpactClip();

        assertEquals(0F, clip.silhouette.get());
        assertEquals(-1, clip.target.get());
        assertNotNull(clip.silhouetteKf);
        assertNotNull(clip.inkBurstKf);
        assertNotNull(clip.shockwaveKf);
        assertNotNull(clip.flashStarKf);

        assertFalse(clip.kfChannels.isEmpty());
        assertEquals(clip.kfChannels.size(), clip.kfNames.size());
    }

    @Test
    void effectDetectionCoversShockwaveAndInk()
    {
        BbsVfxImpactState.reset();
        BbsVfxImpactState.active = true;

        BbsVfxImpactState.inkBurst = 0.8F;
        assertTrue(BbsVfxImpactState.hasEffect());

        BbsVfxImpactState.inkBurst = 0F;
        assertFalse(BbsVfxImpactState.hasEffect());

        BbsVfxImpactState.shockwave = 0.3F;
        assertTrue(BbsVfxImpactState.hasEffect());

        BbsVfxImpactState.shockwave = 0F;
        BbsVfxImpactState.flashStar = 0.9F;
        assertTrue(BbsVfxImpactState.hasEffect());
    }

    @Test
    @SuppressWarnings("unchecked")
    void applyClip_scalingAndKeyframeOverride()
    {
        ImpactClip clip = new ImpactClip();
        clip.duration.set(20);
        clip.silhouette.set(0.8F);
        clip.target.set(2);
        clip.flashStar.set(1.0F);

        ClipContext context = new ClipContext()
        {
            @Override
            public boolean apply(Clip c, Object pos)
            {
                return false;
            }
        };
        context.setup(5, 5, 0F);

        Position pos = new Position();
        clip.apply(context, pos);

        assertTrue(BbsVfxImpactState.active);
        assertEquals(0.8F, BbsVfxImpactState.silhouette, 1e-4F);
        assertEquals(2, BbsVfxImpactState.silTarget);
        assertEquals(1.0F, BbsVfxImpactState.flashStar, 1e-4F);

        // Keyframe override: when channel has a keyframe at t=5, it overrides the slider
        clip.silhouetteKf.insert(5.0F, 0.35D);
        clip.apply(context, pos);
        assertEquals(0.35F, BbsVfxImpactState.silhouette, 1e-4F);
    }
}