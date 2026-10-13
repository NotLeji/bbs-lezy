package leji.bbslezy.ui;

import mchorse.bbs_mod.forms.forms.Form;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Reads BBS-POV's hand-playback suppress state via reflection (no compile dep).
 *
 * <p>POV cancels {@code FormRenderer.applyTransforms} for forms it drives as first-person hands.
 * LOD must not cull those forms: POV already hides what it needs, and culling a POV-driven form
 * would fight its visibility handling. Read-only, fail-open when POV is absent.</p>
 */
public class LezyPov
{
    private static volatile boolean resolved;
    private static Method isSuppressMethod;

    public static boolean isLoaded()
    {
        try
        {
            return FabricLoader.getInstance().isModLoaded("bbs_pov");
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /**
     * Whether POV currently suppresses transforms for this form (drives it as FP hands).
     * LOD skips culling such forms.
     */
    public static boolean isSuppressed(Form form)
    {
        if (form == null || !isLoaded() || !resolve())
        {
            return false;
        }

        try
        {
            return Boolean.TRUE.equals(isSuppressMethod.invoke(null, form));
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static boolean resolve()
    {
        if (resolved)
        {
            return isSuppressMethod != null;
        }

        resolved = true;

        try
        {
            Class<?> playback = Class.forName("Glaxium.POV.hand.playback.PovHandPlayback");
            isSuppressMethod = playback.getMethod("isSuppressFormTransform", Form.class);
        }
        catch (Throwable ignored)
        {
            isSuppressMethod = null;
        }

        return isSuppressMethod != null;
    }
}
