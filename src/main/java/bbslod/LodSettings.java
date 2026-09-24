package bbslod;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * The addon's client settings, assigned in the settings consumer (BBS's own pattern, cf.
 * {@code BBSSettings}). The file lands at {@code <bbs settings folder>/bbslezy.json} and is edited
 * through BBS's own settings screen — no UI work in this addon.
 */
public class LodSettings
{
    public static ValueBoolean enabled;
    public static ValueInt renderLimit;
    public static ValueDouble focusDistance;
    public static ValueBoolean separateAudioTracks;

    public static void register(SettingsBuilder builder)
    {
        builder.category("general", Icons.GEAR);

        enabled = builder.getBoolean("enabled", true);
        renderLimit = builder.getInt("render_limit", 100, 0, 2000);
        focusDistance = builder.getDouble("focus_distance", 0D, 0D, 256D);
        separateAudioTracks = builder.getBoolean("separate_audio_tracks", false);
    }
}
