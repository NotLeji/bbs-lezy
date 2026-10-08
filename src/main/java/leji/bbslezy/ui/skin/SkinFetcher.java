package leji.bbslezy.ui.skin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class SkinFetcher
{
    /**
     * Determines whether the given skin file uses the slim (Alex) model
     * by checking transparency in the extra arm pixels.
     */
    public static boolean isAlex(File png)
    {
        if (png == null || !png.exists())
        {
            return false;
        }
        try
        {
            BufferedImage img = ImageIO.read(png);
            if (img == null)
            {
                return false;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            if (h < 64)
            {
                return false;
            }
            int scale = w / 64;
            int a1 = alpha(img, 54 * scale, 20 * scale);
            int a2 = alpha(img, 46 * scale, 52 * scale);
            return a1 == 0 && a2 == 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static int alpha(BufferedImage img, int x, int y)
    {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight())
        {
            return -1;
        }
        return (img.getRGB(x, y) >> 24) & 0xFF;
    }
}
