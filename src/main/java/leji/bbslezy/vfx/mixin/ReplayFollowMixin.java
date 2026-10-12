package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stores the per-actor "follow curve" configuration on the {@link Replay}.
 */
@Mixin(value = Replay.class, remap = false)
public abstract class ReplayFollowMixin
{
    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addFollow(String id, CallbackInfo ci)
    {
        ValueGroup self = (ValueGroup) (Object) this;

        self.add(new ValueBoolean("xavin_follow_enabled", false));
        self.add(new ValueInt("xavin_follow_target", -1));
        self.add(new ValueBoolean("xavin_follow_align", true));
    }
}
