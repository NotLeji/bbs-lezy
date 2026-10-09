package leji.bbslezy.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class TransitionClip extends CameraClip
{
    public static final int TYPE_FADE_OUT = 0;
    public static final int TYPE_FADE_IN = 1;
    public static final int TYPE_DIP_TO_BLACK = 2;
    public static final int TYPE_FLASH = 3;
    public static final int TYPE_CROSSFADE = 4;

    public final ValueInt type = new ValueInt("type", TYPE_FADE_OUT, 0, 4);
    public final ValueColor color = new ValueColor("color", Color.rgba(Colors.A100));
    public final ValueFloat intensity = new ValueFloat("intensity", 1F);
    public final Interpolation interp = new Interpolation("interp", Interpolations.MAP);

    public final KeyframeChannel<Double> channelIntensity = new KeyframeChannel<>("intensity", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> channelColor = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel[] channels;

    private final TintEffect effect = new TintEffect();

    public TransitionClip()
    {
        super();

        this.channels = new KeyframeChannel[] {
            this.channelIntensity,
            this.channelColor,
        };

        this.add(this.type);
        this.add(this.color);
        this.add(this.intensity);
        this.add(this.interp);

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        int dur = Math.max(1, this.duration.get());
        float progress = MathUtils.clamp(t / (float) dur, 0F, 1F);

        float opacity;
        int currentType = this.type.get();

        if (currentType == TYPE_FADE_OUT)
        {
            opacity = (float) this.interp.interpolate(IInterp.context.set(0D, 1D, progress));
        }
        else if (currentType == TYPE_FADE_IN)
        {
            opacity = (float) this.interp.interpolate(IInterp.context.set(1D, 0D, progress));
        }
        else if (currentType == TYPE_DIP_TO_BLACK)
        {
            if (progress < 0.5F)
            {
                opacity = (float) this.interp.interpolate(IInterp.context.set(0D, 1D, progress / 0.5F));
            }
            else
            {
                opacity = (float) this.interp.interpolate(IInterp.context.set(1D, 0D, (progress - 0.5F) / 0.5F));
            }
        }
        else if (currentType == TYPE_FLASH)
        {
            float inv = 1F - progress;
            opacity = inv * inv;
        }
        else
        {
            /* CROSSFADE: linear fade out over the clip duration */
            opacity = 1F - progress;
        }

        float intens = this.channelIntensity.isEmpty()
            ? this.intensity.get()
            : (float) (double) this.channelIntensity.interpolate(t);

        opacity = MathUtils.clamp(opacity * intens * factor, 0F, 1F);

        if (opacity > 0.001F)
        {
            Color col = this.channelColor.isEmpty()
                ? this.color.get()
                : this.channelColor.interpolate(t, this.color.get());

            int c = col.getARGBColor();
            if (currentType == TYPE_FLASH && (c & Colors.RGB) == 0)
            {
                c = Colors.WHITE;
            }

            this.effect.layer = this.layer.get();
            this.effect.renderOrder = context.count;
            this.effect.color = Colors.setA(c, opacity);

            TintClip.getTints(context).add(this.effect);
        }
    }

    @Override
    protected Clip create()
    {
        return new TransitionClip();
    }
}
