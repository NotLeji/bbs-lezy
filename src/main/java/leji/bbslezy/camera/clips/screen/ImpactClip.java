package leji.bbslezy.camera.clips.screen;

import leji.bbslezy.camera.clips.modifiers.LezyNoise;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ImpactClip extends CameraClip
{
    public final ValueInt style = new ValueInt("style", ImpactStyle.ANIME_IMPACT.ordinal(), 0, ImpactStyle.values().length - 1);

    /* Freeze frame */
    public final ValueBoolean freezeEnabled = new ValueBoolean("freezeEnabled", true);
    public final ValueInt freezeDuration = new ValueInt("freezeDuration", 3, 0, 100);

    /* Flash */
    public final ValueBoolean flashEnabled = new ValueBoolean("flashEnabled", true);
    public final ValueColor flashColor = new ValueColor("flashColor", Color.rgba(Colors.WHITE));
    public final ValueFloat flashIntensity = new ValueFloat("flashIntensity", 1.0F);
    public final ValueInt flashDuration = new ValueInt("flashDuration", 4, 1, 100);

    /* Punch-in FOV */
    public final ValueBoolean punchInEnabled = new ValueBoolean("punchInEnabled", true);
    public final ValueFloat punchInFov = new ValueFloat("punchInFov", 15.0F);
    public final ValueInt punchInDuration = new ValueInt("punchInDuration", 10, 1, 100);

    /* Impact shake */
    public final ValueBoolean shakeEnabled = new ValueBoolean("shakeEnabled", true);
    public final ValueFloat shakeIntensity = new ValueFloat("shakeIntensity", 1.5F);
    public final ValueInt shakeDuration = new ValueInt("shakeDuration", 8, 1, 100);

    /* Keyframe animation */
    public final KeyframeChannel<Double> channelIntensity = new KeyframeChannel<>("intensity", KeyframeFactories.DOUBLE);
    public final KeyframeChannel[] channels;

    private final TintEffect effect = new TintEffect();

    public ImpactClip()
    {
        super();

        this.channels = new KeyframeChannel[] {
            this.channelIntensity,
        };

        this.add(this.style);

        this.add(this.freezeEnabled);
        this.add(this.freezeDuration);

        this.add(this.flashEnabled);
        this.add(this.flashColor);
        this.add(this.flashIntensity);
        this.add(this.flashDuration);

        this.add(this.punchInEnabled);
        this.add(this.punchInFov);
        this.add(this.punchInDuration);

        this.add(this.shakeEnabled);
        this.add(this.shakeIntensity);
        this.add(this.shakeDuration);

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }
    }

    public void applyStyle(ImpactStyle stylePreset)
    {
        this.style.set(stylePreset.ordinal());
        if (stylePreset == ImpactStyle.CUSTOM)
        {
            return;
        }

        this.freezeEnabled.set(stylePreset.freeze);
        this.freezeDuration.set(stylePreset.freezeDuration);

        this.flashEnabled.set(stylePreset.flash);
        this.flashColor.set(Color.rgba(stylePreset.flashColor));
        this.flashIntensity.set(stylePreset.flashIntensity);
        this.flashDuration.set(stylePreset.flashDuration);

        this.punchInEnabled.set(stylePreset.punchIn);
        this.punchInFov.set(stylePreset.punchInFov);
        this.punchInDuration.set(stylePreset.punchInDuration);

        this.shakeEnabled.set(stylePreset.shake);
        this.shakeIntensity.set(stylePreset.shakeIntensity);
        this.shakeDuration.set(stylePreset.shakeDuration);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        if (factor <= 1e-4F)
        {
            return;
        }

        float globalIntensity = this.channelIntensity.isEmpty()
            ? 1.0F
            : (float) (double) this.channelIntensity.interpolate(t);
        globalIntensity *= factor;

        if (globalIntensity <= 1e-4F)
        {
            return;
        }

        /* 1. Freeze frame */
        if (this.freezeEnabled.get() && t < this.freezeDuration.get())
        {
            context.applyUnderneath(this.tick.get(), 0F, position);
        }

        /* 2. Punch-in FOV */
        if (this.punchInEnabled.get() && t < this.punchInDuration.get() && this.punchInFov.get() > 0F)
        {
            float p = MathUtils.clamp(t / (float) Math.max(1, this.punchInDuration.get()), 0F, 1F);
            float decay = (1F - p) * (1F - p);
            position.angle.fov = Math.max(1F, position.angle.fov - this.punchInFov.get() * decay * globalIntensity);
        }

        /* 3. Camera shake impulse */
        if (this.shakeEnabled.get() && t < this.shakeDuration.get() && this.shakeIntensity.get() > 0F)
        {
            float p = MathUtils.clamp(t / (float) Math.max(1, this.shakeDuration.get()), 0F, 1F);
            float decay = (float) Math.pow(1D - p, 1.5D);
            float amp = this.shakeIntensity.get() * decay * globalIntensity;
            int seed = this.tick.get() * 31;
            double x = t * 0.4D;

            position.point.x += (float) (LezyNoise.fbm(x, seed + 101, 3, 0.5D) * 0.12D * amp);
            position.point.y += (float) (LezyNoise.fbm(x, seed + 211, 3, 0.5D) * 0.20D * amp);
            position.point.z += (float) (LezyNoise.fbm(x, seed + 307, 3, 0.5D) * 0.15D * amp);
            position.angle.yaw += (float) (LezyNoise.fbm(x, seed + 401, 3, 0.5D) * 1.0D * amp);
            position.angle.pitch += (float) (LezyNoise.fbm(x, seed + 503, 3, 0.5D) * 1.0D * amp);
            position.angle.roll += (float) (LezyNoise.fbm(x, seed + 601, 3, 0.5D) * 0.5D * amp);
        }

        /* 4. Impact flash tint */
        if (this.flashEnabled.get() && t < this.flashDuration.get())
        {
            float p = MathUtils.clamp(t / (float) Math.max(1, this.flashDuration.get()), 0F, 1F);
            float decay = (1F - p) * (1F - p);
            float opacity = MathUtils.clamp(this.flashIntensity.get() * decay * globalIntensity, 0F, 1F);

            if (opacity > 0.001F)
            {
                this.effect.layer = this.layer.get();
                this.effect.renderOrder = context.count;
                this.effect.color = Colors.setA(this.flashColor.get().getARGBColor(), opacity);

                TintClip.getTints(context).add(this.effect);
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new ImpactClip();
    }
}
