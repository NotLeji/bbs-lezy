package leji.bbslezy.vfx.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-soft layer for the VFX half of the addon.
 *
 * <p>VFX was ported from BBSVFX, which has no 1.21.1 release of its own — every VFX hook here
 * targets BBS 2.7 internals by hand. The mixin config is {@code required:false} with
 * {@code defaultRequire:0}, so a stale hook disappears silently instead of failing the boot. This
 * class makes that visible and keeps the rest of the addon usable:</p>
 *
 * <ul>
 *   <li>{@link #probe()} runs once at client init and logs which VFX hooks actually landed, so a
 *       missing one is a line in the log instead of a feature that "mysteriously does nothing".</li>
 *   <li>{@link #fail(String, Throwable)} is called from the post-render composite chain: the first
 *       throw disables that chain for the session and logs the stack trace. A broken VFX pass
 *       degrades to "no VFX" rather than crashing every frame of the film.</li>
 * </ul>
 */
public final class LezyVfxSupport
{
    private static final Logger LOGGER = LoggerFactory.getLogger("bbslezy/vfx");

    /** Duck interfaces mixed onto BBS classes; absent == that VFX feature is dead on this build. */
    private static final String[][] HOOKS = {
        {"smear", "leji.bbslezy.vfx.forms.ISmearHolder", "mchorse.bbs_mod.forms.forms.Form"},
        {"smear (whole form)", "leji.bbslezy.vfx.forms.IFormSmear", "mchorse.bbs_mod.forms.forms.Form"},
        {"smear (model channels)", "leji.bbslezy.vfx.forms.IModelSmearChannels", "mchorse.bbs_mod.forms.forms.ModelForm"},
        {"smear (bone)", "leji.bbslezy.vfx.forms.ISmearBone", "mchorse.bbs_mod.utils.pose.PoseTransform"},
        {"blend (bone)", "leji.bbslezy.vfx.forms.IBlendBone", "mchorse.bbs_mod.utils.pose.PoseTransform"},
        {"motion lines", "leji.bbslezy.vfx.client.IMotionLineDrawer", "mchorse.bbs_mod.forms.renderers.ModelFormRenderer"},
        {"arc trail", "leji.bbslezy.vfx.client.IArcTrailDrawer", "mchorse.bbs_mod.forms.renderers.ModelFormRenderer"},
        {"smear editor", "leji.bbslezy.vfx.client.ISmearPoseEditor", "mchorse.bbs_mod.ui.utils.pose.UIPoseEditor"},
        {"blend editor", "leji.bbslezy.vfx.client.IBlendPoseEditor", "mchorse.bbs_mod.ui.utils.pose.UIPoseEditor"},
    };

    private static volatile boolean compositeFailed;

    private LezyVfxSupport()
    {}

    /** Logs one line per missing hook. Called once from {@code BBSClientReadyEvent}. */
    public static void probe()
    {
        ClassLoader loader = LezyVfxSupport.class.getClassLoader();
        List<String> missing = new ArrayList<>();

        for (String[] hook : HOOKS)
        {
            try
            {
                Class<?> iface = Class.forName(hook[1], false, loader);
                Class<?> target = Class.forName(hook[2], false, loader);

                if (!iface.isAssignableFrom(target))
                {
                    missing.add(hook[0]);
                }
            }
            catch (Throwable t)
            {
                missing.add(hook[0]);
            }
        }

        if (missing.isEmpty())
        {
            LOGGER.info("[bbslezy] VFX hooks: {}/{} applied (BBS {}, MC {})",
                HOOKS.length, HOOKS.length, bbsVersion(), mcVersion());

            return;
        }

        LOGGER.warn("[bbslezy] VFX is partially unavailable on BBS {} (MC {}): {} of {} hooks did not apply -> {}",
            bbsVersion(), mcVersion(), missing.size(), HOOKS.length, String.join(", ", missing));
        LOGGER.warn("[bbslezy] Affected VFX features are disabled; the rest of the addon keeps working.");
    }

    /**
     * Records a failed post-render VFX step. The first failure disables the whole composite chain
     * for this session: repeating a GL error every frame is worse than losing the effect.
     */
    public static void fail(String name, Throwable t)
    {
        if (compositeFailed)
        {
            return;
        }

        compositeFailed = true;
        LOGGER.error("[bbslezy] VFX composite '{}' failed; VFX disabled for this session", name, t);
    }

    /** True once a VFX composite step threw; the UI can use it to say why nothing draws. */
    public static boolean isCompositeFailed()
    {
        return compositeFailed;
    }

    private static String bbsVersion()
    {
        return FabricLoader.getInstance().getModContainer("bbs")
            .map((mod) -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("absent");
    }

    private static String mcVersion()
    {
        return FabricLoader.getInstance().getModContainer("minecraft")
            .map((mod) -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("?");
    }
}
