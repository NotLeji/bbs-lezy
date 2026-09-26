package bbslezy.actions;

import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.utils.clips.Clip;

/**
 * Attack clip recorded from projectile hits during mob autocapture.
 * Playback treats this like {@link AttackActionClip}.
 */
public class ProjectileAttackActionClip extends AttackActionClip
{
    @Override
    protected Clip create()
    {
        return new ProjectileAttackActionClip();
    }
}
