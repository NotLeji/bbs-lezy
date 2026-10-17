package leji.bbslezy;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueString;
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

    public static ValueBoolean perf_enabled;
    public static ValueInt perf_throttle_distance;
    public static ValueInt perf_skip_distance;
    public static ValueInt perf_throttle_ticks;

    public static ValueBoolean nightVision;

    public static ValueBoolean separateAudioTracks;
    public static ValueBoolean openFolderOnImport;
    public static ValueInt bakingBatchPercent;
    public static ValueInt videoCqp;
    public static ValueInt videoCodec;
    public static ValueBoolean hardwareAcceleration;
    public static ValueInt gpuVendor;
    public static ValueBoolean snapPlayerToPreview;
    public static ValueBoolean guidesActionSafe;
    public static ValueBoolean guidesTitleSafe;
    public static ValueBoolean guidesVertical;
    public static ValueBoolean guidesCinematic;
    public static ValueInt guidesColor;
    public static ValueBoolean guidesShowLabels;

    public static ValueInt destructionRoughness;
    public static ValueInt vfxQuality;
    public static ValueBoolean vfxDynamicQuality;

    public static ValueBoolean ui_async_keys;
    public static ValueBoolean ui_async_film_load;
    public static ValueBoolean ui_world_skip;

    public static void register(SettingsBuilder builder)
    {
        builder.category("general", Icons.GEAR);

        enabled = builder.getBoolean("enabled", true);
        renderLimit = builder.getInt("render_limit", 100, 0, 2000);
        focusDistance = builder.getDouble("focus_distance", 0D, 0D, 256D);

        perf_enabled = builder.getBoolean("perf_enabled", false);
        perf_throttle_distance = builder.getInt("perf_throttle_distance", 32, 0, 512);
        perf_skip_distance = builder.getInt("perf_skip_distance", 96, 0, 1024);
        perf_throttle_ticks = builder.getInt("perf_throttle_ticks", 4, 0, 20);

        nightVision = builder.getBoolean("night_vision", false);

        separateAudioTracks = builder.getBoolean("separate_audio_tracks", false);
        openFolderOnImport = builder.getBoolean("open_folder_on_import", false);
        bakingBatchPercent = builder.getInt("baking_batch_percent", 5, 1, 100);
        videoCqp = builder.getInt("video_cqp", 18, 0, 51).slider();
        videoCodec = builder.getInt("video_codec", 0, 0, 2).modes(
            IKey.raw("H.264 (MP4)"),
            IKey.raw("H.265 / HEVC (MP4)"),
            IKey.raw("VP9 (WebM)")
        );
        hardwareAcceleration = builder.getBoolean("hardware_acceleration", true);
        gpuVendor = builder.getInt("gpu_vendor", 0, 0, 3).modes(
            IKey.raw("Auto-Detect GPU"),
            IKey.raw("NVIDIA (NVENC)"),
            IKey.raw("AMD (AMF)"),
            IKey.raw("Intel (QSV)")
        );
        snapPlayerToPreview = builder.getBoolean("snap_player_to_preview", true);

        guidesActionSafe = builder.getBoolean("guides_action_safe", false);
        guidesTitleSafe = builder.getBoolean("guides_title_safe", false);
        guidesVertical = builder.getBoolean("guides_vertical", false);
        guidesCinematic = builder.getBoolean("guides_cinematic", false);
        guidesColor = builder.getInt("guides_color", 0x88FFFFFF).colorAlpha();
        guidesShowLabels = builder.getBoolean("guides_show_labels", false);
        destructionRoughness = builder.getInt("destruction_roughness", 0, 0, 100);
        vfxQuality = builder.getInt("vfx_quality", 0, 0, 3).modes(
            IKey.raw("Auto"),
            IKey.raw("High"),
            IKey.raw("Medium"),
            IKey.raw("Low")
        );
        vfxDynamicQuality = builder.getBoolean("vfx_dynamic_quality", true);

        ui_async_keys = builder.getBoolean("ui_async_keys", true);
        ui_async_film_load = builder.getBoolean("ui_async_film_load", true);
        ui_world_skip = builder.getBoolean("ui_world_skip", true);
    }
}
