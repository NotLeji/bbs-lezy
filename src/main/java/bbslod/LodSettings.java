package bbslod;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * The addon's client settings, assigned in the settings consumer (BBS's own pattern, cf.
 * {@code BBSSettings}). The file lands at {@code <bbs settings folder>/bbslod.json} and is edited
 * through BBS's own settings screen — no UI work in this addon.
 */
public class LodSettings
{
    public static ValueBoolean enabled;
    public static ValueFloat cullDistance;
    public static ValueFloat simplifyDistance;
    public static ValueInt boneCullDepth;
    public static ValueFloat boneCullSize;
    public static ValueBoolean fovBias;
    public static ValueBoolean occlusion;
    public static ValueFloat occlusionBias;
    public static ValueBoolean debug;

    public static void register(SettingsBuilder builder)
    {
        builder.category("general", Icons.GEAR);

        enabled = builder.getBoolean("enabled", true);
        cullDistance = builder.getFloat("cull_distance", 128F, 0F, 1024F);
        simplifyDistance = builder.getFloat("simplify_distance", 64F, 0F, 1024F);
        boneCullDepth = builder.getInt("bone_cull_depth", 0, 0, 8);
        boneCullSize = builder.getFloat("bone_cull_size", 0.02F, 0F, 0.2F);
        fovBias = builder.getBoolean("fov_bias", true);
        occlusion = builder.getBoolean("occlusion", false);
        occlusionBias = builder.getFloat("occlusion_bias", 0.5F, 0F, 8F);
        debug = builder.getBoolean("debug", false);
    }
}
