package leji.bbslezy.vfx.forms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BlendMode enum + step interpolation of the blend_mode channel (Port BBSVFX D6). */
class BlendModeTest
{
    @Test
    void byIndexClampsUnknownToNormal()
    {
        assertSame(BlendMode.NORMAL, BlendMode.byIndex(0));
        assertSame(BlendMode.ADD, BlendMode.byIndex(BlendMode.ADD.ordinal()));
        assertSame(BlendMode.NORMAL, BlendMode.byIndex(-1));
        assertSame(BlendMode.NORMAL, BlendMode.byIndex(9999));
    }

    @Test
    void displayNamesFollowDeclarationOrder()
    {
        assertEquals(BlendMode.values().length, BlendMode.DISPLAY_NAMES.size());
        assertEquals("Normal", BlendMode.DISPLAY_NAMES.get(0));
        assertEquals("Add", BlendMode.DISPLAY_NAMES.get(1));

        for (int i = 0; i < BlendMode.values().length; i++)
        {
            assertEquals(BlendMode.values()[i].display, BlendMode.DISPLAY_NAMES.get(i));
        }
    }

    @Test
    void fixedFunctionFlagMatchesGlSupport()
    {
        assertTrue(BlendMode.NORMAL.fixedFunction);
        assertTrue(BlendMode.ADD.fixedFunction);
        assertTrue(BlendMode.SCREEN.fixedFunction);
        assertTrue(BlendMode.MULTIPLY.fixedFunction);
        assertTrue(BlendMode.DARKEN.fixedFunction);
        assertTrue(BlendMode.LIGHTEN.fixedFunction);
        assertFalse(BlendMode.OVERLAY.fixedFunction);
        assertFalse(BlendMode.DIFFERENCE.fixedFunction);
    }

    @Test
    void blendModeChannelStepsInsteadOfTweening()
    {
        BlendModeKeyframeFactory factory = new BlendModeKeyframeFactory();

        /* A blend mode is a discrete category: between keyframes it must hold the LEFT value
         * (Add here), never a tween through intermediate mode indices. */
        assertEquals(
            Integer.valueOf(BlendMode.ADD.ordinal()),
            factory.interpolate(BlendMode.NORMAL.ordinal(), BlendMode.ADD.ordinal(), BlendMode.MULTIPLY.ordinal(), BlendMode.SCREEN.ordinal(), null, 0.5F));

        assertEquals(
            Integer.valueOf(BlendMode.MULTIPLY.ordinal()),
            factory.interpolate(BlendMode.ADD.ordinal(), BlendMode.MULTIPLY.ordinal(), BlendMode.SCREEN.ordinal(), BlendMode.SCREEN.ordinal(), null, 0.99F));
    }

    @Test
    void wholeBoneSentinelIsStableLegacyKey()
    {
        /* Saved blend poses carry this key; renaming it would drop whole-model blend from old films. */
        assertEquals("xavin$whole", IBlendBone.WHOLE_BONE);
    }
}
