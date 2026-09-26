package bbslezy.actions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionClipsTest
{
    @Test
    void mobDeath_isClientOnly()
    {
        MobDeathActionClip clip = new MobDeathActionClip();
        assertTrue(clip.isClient(), "MobDeathActionClip must be client-only");
        assertNotNull(clip.create());
        assertInstanceOf(MobDeathActionClip.class, clip.create());
    }

    @Test
    void projectileAttack_createsSelf()
    {
        ProjectileAttackActionClip clip = new ProjectileAttackActionClip();
        assertNotNull(clip.create());
        assertInstanceOf(ProjectileAttackActionClip.class, clip.create());
    }
}
