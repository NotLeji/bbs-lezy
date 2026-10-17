package leji.bbslezy.vfx.camera;

import org.junit.jupiter.api.AfterEach;
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
}
