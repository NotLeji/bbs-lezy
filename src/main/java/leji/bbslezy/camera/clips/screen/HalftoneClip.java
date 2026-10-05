package leji.bbslezy.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.List;

public class HalftoneClip extends CameraClip
{
    public static final double DEFAULT_CELLS = 130.0;
    public static final double DEFAULT_HIGHLIGHT_THRESHOLD = 0.85;
    public static final double DEFAULT_DOT_SOFTNESS = 0.05;
    public static final double DEFAULT_ANGLE = 45.0;

    public ValueInt mode = new ValueInt("mode", HalftoneEffect.MODE_BW);
    public ValueInt inkColor = new ValueInt("inkColor", Colors.A100);

    public final KeyframeChannel<Double> cells = new KeyframeChannel<>("cells", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> highlightThreshold = new KeyframeChannel<>("highlight_threshold", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> dotSoftness = new KeyframeChannel<>("dot_softness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> angle = new KeyframeChannel<>("angle", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private HalftoneEffect effect = new HalftoneEffect();

    public static List<HalftoneEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("halftoneEffects", ArrayList::new);
    }

    public HalftoneClip()
    {
        this.channels = new KeyframeChannel[] {
            this.cells,
            this.highlightThreshold,
            this.dotSoftness,
            this.angle
        };

        this.add(this.mode);
        this.add(this.inkColor);
        this.add(this.cells);
        this.add(this.highlightThreshold);
        this.add(this.dotSoftness);
        this.add(this.angle);
    }

    private static float interpolateOrDefault(KeyframeChannel<Double> channel, float tick, double fallback)
    {
        return channel.isEmpty() ? (float) fallback : (float) (double) channel.interpolate(tick);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;

        this.effect.reset();
        this.effect.layer = this.layer.get();
        this.effect.renderOrder = context.count;
        this.effect.mode = this.mode.get();
        this.effect.inkColor = this.inkColor.get();
        this.effect.cells = Math.max(1F, interpolateOrDefault(this.cells, t, DEFAULT_CELLS));
        this.effect.highlightThreshold = Math.max(0.01F, interpolateOrDefault(this.highlightThreshold, t, DEFAULT_HIGHLIGHT_THRESHOLD));
        this.effect.dotSoftness = Math.max(0.001F, interpolateOrDefault(this.dotSoftness, t, DEFAULT_DOT_SOFTNESS));
        this.effect.angle = interpolateOrDefault(this.angle, t, DEFAULT_ANGLE);

        getEffects(context).add(this.effect);
    }

    @Override
    protected Clip create()
    {
        return new HalftoneClip();
    }
}
