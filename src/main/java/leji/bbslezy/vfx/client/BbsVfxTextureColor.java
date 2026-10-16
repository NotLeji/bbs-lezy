package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes (and caches) the average opaque colour of an actor's texture.
 */
public final class BbsVfxTextureColor
{
    private static final int WHITE = 0xFFFFFFFF;

    private static final Map<Link, Integer> CACHE = new HashMap<>();

    private BbsVfxTextureColor()
    {}

    public static int average(Link link)
    {
        if (link == null)
        {
            return WHITE;
        }

        Integer cached = CACHE.get(link);

        if (cached != null)
        {
            return cached;
        }

        int argb = WHITE;
        Pixels pixels = null;

        try
        {
            pixels = BBSModClient.getTextures().getPixels(link);

            if (pixels != null)
            {
                double r = 0D;
                double g = 0D;
                double b = 0D;
                double a = 0D;

                for (int i = 0, c = pixels.getCount(); i < c; i++)
                {
                    Color color = pixels.getColor(i);

                    if (color == null)
                    {
                        continue;
                    }

                    float w = color.a;

                    r += color.r * w;
                    g += color.g * w;
                    b += color.b * w;
                    a += w;
                }

                if (a > 0D)
                {
                    int ri = clamp((int) Math.round(r / a * 255D));
                    int gi = clamp((int) Math.round(g / a * 255D));
                    int bi = clamp((int) Math.round(b / a * 255D));

                    argb = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
                }
            }
        }
        catch (Exception e)
        {
            /* Keep white on any failure. */
        }
        finally
        {
            if (pixels != null)
            {
                pixels.delete();
            }
        }

        CACHE.put(link, argb);

        return argb;
    }

    private static int clamp(int v)
    {
        return Math.max(0, Math.min(255, v));
    }
}
