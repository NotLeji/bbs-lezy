package leji.bbslezy.vfx.client;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Smear-frame dissolve for block forms through vanilla block atlas.
 */
public final class BbsVfxAtlasDissolve
{
    private static final int LEVELS = 16;
    private static final int CELL = 3;

    private static final Map<Integer, AbstractTexture> CACHE = new HashMap<>();
    private static final Map<Integer, AbstractTexture> PENDING = new LinkedHashMap<>();
    private static final Set<Integer> FAILED = new HashSet<>();

    private BbsVfxAtlasDissolve()
    {}

    public static AbstractTexture get(AbstractTexture original, float dissolve)
    {
        if (original == null || dissolve <= 0F)
        {
            return original;
        }

        int level = Math.max(1, Math.min(LEVELS, Math.round(dissolve * LEVELS)));
        AbstractTexture cached = CACHE.get(level);

        if (cached != null)
        {
            return cached;
        }

        if (!FAILED.contains(level) && !PENDING.containsKey(level))
        {
            PENDING.put(level, original);
        }

        return original;
    }

    public static void processPending()
    {
        if (PENDING.isEmpty())
        {
            return;
        }

        for (Map.Entry<Integer, AbstractTexture> entry : PENDING.entrySet())
        {
            int level = entry.getKey();

            if (CACHE.containsKey(level))
            {
                continue;
            }

            AbstractTexture built = bbslezy$build(entry.getValue(), level / (float) LEVELS);

            if (built != null)
            {
                CACHE.put(level, built);
            }
            else
            {
                FAILED.add(level);
            }
        }

        PENDING.clear();
    }

    private static AbstractTexture bbslezy$build(AbstractTexture original, float dissolve)
    {
        try
        {
            original.bindTexture();

            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            if (w < 2 || h < 2)
            {
                return null;
            }

            NativeImage image = new NativeImage(w, h, false);
            image.loadFromTextureImage(0, false);

            for (int y = 0; y < h; y++)
            {
                for (int x = 0; x < w; x++)
                {
                    int abgr = image.getColor(x, y);
                    int alpha = (abgr >>> 24) & 0xFF;

                    if (alpha > 0 && bbslezy$hash(x / CELL, y / CELL) < dissolve)
                    {
                        image.setColor(x, y, abgr & 0x00FFFFFF);
                    }
                }
            }

            return new NativeImageBackedTexture(image);
        }
        catch (Throwable e)
        {
            return null;
        }
    }

    private static float bbslezy$hash(int cx, int cy)
    {
        double s = Math.sin(cx * 127.1 + cy * 311.7) * 43758.5453;

        return (float) (s - Math.floor(s));
    }
}
