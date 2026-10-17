package leji.bbslezy.mixin.client;

import leji.bbslezy.ui.LezyPreviewSnap;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.controller.FilmKeyframeInsertion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FilmKeyframeInsertion.class, remap = false)
public abstract class FilmKeyframeInsertionMixin
{
    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getX()D")
    )
    private static double bbslezy$redirectSnapX(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? LezyPreviewSnap.snapshot[0] : player.getX();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getY()D")
    )
    private static double bbslezy$redirectSnapY(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? LezyPreviewSnap.snapshot[1] : player.getY();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getZ()D")
    )
    private static double bbslezy$redirectSnapZ(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? LezyPreviewSnap.snapshot[2] : player.getZ();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getYaw()F")
    )
    private static float bbslezy$redirectSnapYaw(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? (float) LezyPreviewSnap.snapshot[3] : player.getYaw();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getPitch()F")
    )
    private static float bbslezy$redirectSnapPitch(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? (float) LezyPreviewSnap.snapshot[4] : player.getPitch();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getHeadYaw()F")
    )
    private static float bbslezy$redirectSnapHeadYaw(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? (float) LezyPreviewSnap.snapshot[3] : player.getHeadYaw();
    }

    @Redirect(
        method = "lambda$insertPlayerFrame$4",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/forms/entities/IEntity;getBodyYaw()F")
    )
    private static float bbslezy$redirectSnapBodyYaw(IEntity player)
    {
        return LezyPreviewSnap.active && LezyPreviewSnap.snapshot != null ? (float) LezyPreviewSnap.snapshot[3] : player.getBodyYaw();
    }
}
