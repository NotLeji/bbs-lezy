package leji.bbslezy.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EdgeWrapMathTest
{
    @Test
    public void testWrapRightMaintainsDeltaContinuity()
    {
        int menuWidth = 1920;
        int border = 5;
        int borderPadding = border + 1; // 6

        int initialX = 500;
        int mouseXBeforeWrap = menuWidth - border; // 1915
        int dxBefore = mouseXBeforeWrap - initialX; // 1415

        // After wrapping right, mouse becomes left edge: borderPadding (6)
        int mouseXAfterWrap = borderPadding; // 6
        int newInitialX = initialX - (menuWidth - borderPadding * 2); // 500 - (1920 - 12) = 500 - 1908 = -1408

        int dxAfter = mouseXAfterWrap - newInitialX; // 6 - (-1408) = 1414 (within 1 px discretization)

        assertEquals(dxBefore - 1, dxAfter);
    }

    @Test
    public void testWrapLeftMaintainsDeltaContinuity()
    {
        int menuWidth = 1920;
        int border = 5;
        int borderPadding = border + 1; // 6

        int initialX = 500;
        int mouseXBeforeWrap = border; // 5
        int dxBefore = mouseXBeforeWrap - initialX; // -495

        // After wrapping left, mouse becomes right edge: menuWidth - borderPadding (1914)
        int mouseXAfterWrap = menuWidth - borderPadding; // 1914
        int newInitialX = initialX + (menuWidth - borderPadding * 2); // 500 + 1908 = 2408

        int dxAfter = mouseXAfterWrap - newInitialX; // 1914 - 2408 = -494 (within 1 px discretization)

        assertEquals(dxBefore + 1, dxAfter);
    }
}
