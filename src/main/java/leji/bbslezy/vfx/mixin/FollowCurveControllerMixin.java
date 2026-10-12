package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import leji.bbslezy.vfx.forms.CurveForm;

import java.util.List;
import java.util.Map;

/**
 * "Follow curve" for actors: snaps actors whose replay has {@code xavin_follow_enabled} onto the target curve.
 */
@Mixin(value = BaseFilmController.class, remap = false)
public abstract class FollowCurveControllerMixin
{
    @Shadow @Final public Map<String, IEntity> entities;
    @Shadow @Final public Film film;

    @Inject(method = "startRenderFrame", at = @At("TAIL"))
    private void bbslezy$followCurves(float transition, CallbackInfo ci)
    {
        if (this.film == null)
        {
            return;
        }

        List<Replay> replays = this.film.replays.getList();

        for (Map.Entry<String, IEntity> entry : this.entities.entrySet())
        {
            Replay replay = this.bbslezy$replayById(replays, entry.getKey());

            if (replay != null)
            {
                this.bbslezy$applyFollow(replays, replay, entry.getValue(), transition);
            }
        }
    }

    private Replay bbslezy$replayById(List<Replay> replays, String id)
    {
        for (Replay replay : replays)
        {
            if (replay.getId().equals(id))
            {
                return replay;
            }
        }

        return null;
    }

    private void bbslezy$applyFollow(List<Replay> replays, Replay replay, IEntity entity, float transition)
    {
        Form form = entity.getForm();

        if (form == null
            || !(replay.get("xavin_follow_enabled") instanceof ValueBoolean enabled) || !enabled.get()
            || !(replay.get("xavin_follow_target") instanceof ValueInt targetValue))
        {
            return;
        }

        int target = targetValue.get();

        if (target < 0 || target >= replays.size())
        {
            return;
        }

        IEntity curveEntity = this.entities.get(replays.get(target).getId());

        if (curveEntity == null || curveEntity == entity || !(curveEntity.getForm() instanceof CurveForm curve))
        {
            return;
        }

        float off = form.get("follow_offset") instanceof ValueFloat offset
            ? (float) Math.max(0D, Math.min(1D, offset.get())) : 0F;

        Vector3f localPos = new Vector3f();
        Vector3f localTangent = new Vector3f();

        curve.arcPoint(off, localPos, localTangent);

        Matrix4f world = FilmMatrices.getMatrixForRenderWithRotation(curveEntity, 0D, 0D, 0D, transition);
        Vector3f worldPos = world.transformPosition(new Vector3f(localPos));

        entity.setPosition(worldPos.x, worldPos.y, worldPos.z);
        entity.setPrevX(worldPos.x);
        entity.setPrevY(worldPos.y);
        entity.setPrevZ(worldPos.z);

        boolean align = !(replay.get("xavin_follow_align") instanceof ValueBoolean alignValue) || alignValue.get();

        if (!align)
        {
            return;
        }

        Vector3f worldTangent = world.transformDirection(new Vector3f(localTangent));

        if (worldTangent.lengthSquared() < 1e-9F)
        {
            return;
        }

        worldTangent.normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-worldTangent.x, worldTangent.z));

        entity.setBodyYaw(yaw);
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setPrevBodyYaw(yaw);
        entity.setPrevYaw(yaw);
        entity.setPrevHeadYaw(yaw);

        float pitch = (float) Math.asin(Math.max(-1F, Math.min(1F, -worldTangent.y)));
        float roll = (float) Math.toRadians(curve.rotationAt(off).z);

        form.transform.get().rotate.x = pitch;
        form.transform.get().rotate.z = roll;
    }
}
