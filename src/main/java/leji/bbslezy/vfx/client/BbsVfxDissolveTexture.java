package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Smear-frame dissolve baked into texture alpha for shader compatibility.
 */
public final class BbsVfxDissolveTexture
{
    private static final int LEVELS = 16;
    private static final float CELLS = 80F;

    private static final Map<Key, Texture> CACHE = new HashMap<>();
    private static final Map<Key, Integer> PENDING = new LinkedHashMap<>();
    private static final Set<Key> FAILED = new HashSet<>();

    private BbsVfxDissolveTexture()
    {}

    public static Texture get(Link link, float dissolve, Texture original)
    {
        if (link == null || dissolve <= 0F)
        {
            return original;
        }

        int level = Math.max(1, Math.min(LEVELS, Math.round(dissolve * LEVELS)));
        Key key = new Key(link, level);
        Texture cached = CACHE.get(key);

        if (cached != null)
        {
            return cached;
        }

        if (!FAILED.contains(key) && !PENDING.containsKey(key))
        {
            PENDING.put(key, original != null ? original.getFilter() : GL11.GL_NEAREST);
        }

        return original;
    }

    public static void processPending()
    {
        if (PENDING.isEmpty())
        {
            return;
        }

        for (Map.Entry<Key, Integer> entry : PENDING.entrySet())
        {
            Key key = entry.getKey();

            if (CACHE.containsKey(key))
            {
                continue;
            }

            Texture built = bbslezy$build(key.link(), key.level() / (float) LEVELS, entry.getValue());

            if (built != null)
            {
                CACHE.put(key, built);
            }
            else
            {
                FAILED.add(key);
            }
        }

        PENDING.clear();
    }

    private static Texture bbslezy$build(Link link, float dissolve, int filter)
    {
        Pixels src = null;

        try
        {
            src = BBSModClient.getTextures().getPixels(link);

            if (src == null || src.width < 2 || src.height < 2)
            {
                return null;
            }

            int w = src.width;
            int h = src.height;
            Pixels out = Pixels.fromSize(w, h);
            Color tmp = new Color();

            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    Color c = src.getColor(x, y);
                    float r = c.r;
                    float g = c.g;
                    float b = c.b;
                    float a = c.a;

                    int cx = (int) Math.floor((x + 0.5F) / w * CELLS);
                    int cy = (int) Math.floor((y + 0.5F) / h * CELLS);

                    if (a > 0F && bbslezy$hash(cx, cy) < dissolve)
                    {
                        a = 0F;
                    }

                    tmp.set(r, g, b, a);
                    out.setColor(x, y, tmp);
                }
            }

            out.getBuffer().rewind();

            return Texture.textureFromPixels(out, filter);
        }
        catch (Throwable e)
        {
            return null;
        }
        finally
        {
            if (src != null)
            {
                src.delete();
            }
        }
    }

    private static float bbslezy$hash(int cx, int cy)
    {
        double s = Math.sin(cx * 127.1 + cy * 311.7) * 43758.5453;

        return (float) (s - Math.floor(s));
    }

    private record Key(Link link, int level)
    {}
}
