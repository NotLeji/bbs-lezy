package leji.bbslezy.vfx.forms;

/**
 * Distinct keyframe-factory instances for the addon's non-pose channels. The UI keyframe-editor registry
 * is keyed by the data factory instance, so a dedicated instance lets a channel get its own editor (here:
 * the {@code blend_mode} dropdown) while still serialising like a plain int. Registered in BOTH
 * {@code KeyframeFactories.FACTORIES} and {@code UIKeyframeFactory}.
 */
public final class LezyVfxKeyframeFactories
{
    public static final BlendModeKeyframeFactory BLEND_MODE = new BlendModeKeyframeFactory();

    private LezyVfxKeyframeFactories()
    {}
}
