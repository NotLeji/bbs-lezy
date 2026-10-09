package leji.bbslezy.camera.clips.modifiers;

import mchorse.bbs_mod.camera.clips.modifiers.ComponentClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public class ProceduralShakeClip extends ComponentClip
{
    public static final int PRESET_GENTLE = 0;
    public static final int PRESET_ACTION = 1;
    public static final int PRESET_EXPLOSION = 2;

    public final ValueInt preset = new ValueInt("preset", PRESET_GENTLE, 0, 2);
    public final ValueFloat frequency = new ValueFloat("frequency", 1F);
    public final ValueInt octaves = new ValueInt("octaves", 3, 1, 6);
    public final ValueInt seed = new ValueInt("seed", 0);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F);

    public final ValueFloat posX = new ValueFloat("posX", 0.12F);
    public final ValueFloat posY = new ValueFloat("posY", 0.12F);
    public final ValueFloat posZ = new ValueFloat("posZ", 0F);
    public final ValueFloat rotYaw = new ValueFloat("rotYaw", 1F);
    public final ValueFloat rotPitch = new ValueFloat("rotPitch", 1F);
    public final ValueFloat rotRoll = new ValueFloat("rotRoll", 0.3F);
    public final ValueFloat rotFov = new ValueFloat("rotFov", 0F);

    public ProceduralShakeClip()
    {
        super();

        this.add(this.preset);
        this.add(this.frequency);
        this.add(this.octaves);
        this.add(this.seed);
        this.add(this.intensity);

        this.add(this.posX);
        this.add(this.posY);
        this.add(this.posZ);
        this.add(this.rotYaw);
        this.add(this.rotPitch);
        this.add(this.rotRoll);
        this.add(this.rotFov);

        /* Default active: yaw (bit 3) and pitch (bit 4) */
        this.active.set(0b0011000);
    }

    public void applyPreset(int presetId)
    {
        this.preset.set(presetId);
        if (presetId == PRESET_GENTLE)
        {
            this.frequency.set(0.8F);
            this.intensity.set(0.4F);
            this.posX.set(0F);
            this.posY.set(0.6F);
            this.posZ.set(0F);
            this.rotYaw.set(0.6F);
            this.rotPitch.set(0.6F);
            this.rotRoll.set(0.1F);
            this.rotFov.set(0F);
        }
        else if (presetId == PRESET_ACTION)
        {
            this.frequency.set(1.6F);
            this.intensity.set(1.0F);
            this.posX.set(0.12F);
            this.posY.set(0.12F);
            this.posZ.set(0F);
            this.rotYaw.set(1.0F);
            this.rotPitch.set(1.0F);
            this.rotRoll.set(0.3F);
            this.rotFov.set(0F);
        }
        else if (presetId == PRESET_EXPLOSION)
        {
            this.frequency.set(3.0F);
            this.intensity.set(2.0F);
            this.posX.set(0.25F);
            this.posY.set(0.25F);
            this.posZ.set(0.25F);
            this.rotYaw.set(1.5F);
            this.rotPitch.set(1.5F);
            this.rotRoll.set(1.5F);
            this.rotFov.set(0.4F);
        }
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

        float speed = Math.max(this.frequency.get(), 1e-4F);
        double x = t * speed * 0.4D;
        int s = this.seed.get();
        int oct = Math.max(1, this.octaves.get());
        float global = Math.max(0F, this.intensity.get()) * factor;

        if (this.isActive(0)) position.point.x += (float) (LezyNoise.fbm(x, s + 101, oct, 0.5D) * this.posX.get() * global);
        if (this.isActive(1)) position.point.y += (float) (LezyNoise.fbm(x, s + 211, oct, 0.5D) * this.posY.get() * global);
        if (this.isActive(2)) position.point.z += (float) (LezyNoise.fbm(x, s + 307, oct, 0.5D) * this.posZ.get() * global);
        if (this.isActive(3)) position.angle.yaw += (float) (LezyNoise.fbm(x, s + 401, oct, 0.5D) * this.rotYaw.get() * global);
        if (this.isActive(4)) position.angle.pitch += (float) (LezyNoise.fbm(x, s + 503, oct, 0.5D) * this.rotPitch.get() * global);
        if (this.isActive(5)) position.angle.roll += (float) (LezyNoise.fbm(x, s + 601, oct, 0.5D) * this.rotRoll.get() * global);
        if (this.isActive(6)) position.angle.fov += (float) (LezyNoise.fbm(x, s + 701, oct, 0.5D) * this.rotFov.get() * global);
    }

    @Override
    protected Clip create()
    {
        return new ProceduralShakeClip();
    }
}
