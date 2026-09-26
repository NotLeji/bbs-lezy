package bbslod;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import bbslezy.actions.CloseContainerActionClip;
import bbslezy.actions.MobDeathActionClip;
import bbslezy.actions.ProjectileAttackActionClip;
import mchorse.bbs_mod.api.events.RegisterActionClipsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.utils.colors.Colors;
/**
 * The common half of the BBS Lezy addon — registered under the {@code bbs-addon} entry point and
 * loaded on both sides.
 *
 * <p>Its client half is {@link BBSLodClient}, and it has to be a separate class: the client events
 * live in BBS's client source set, and a class that so much as mentions one of them cannot be
 * loaded on a dedicated server.</p>
 */
public class BBSLod implements BBSAddonMod
{
    public static final String MOD_ID = "bbslezy";

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        /* Before anything else: a mismatch here reads as "this addon does not fit this BBS build"
         * rather than as a crash on the first thing the user does. */
        BBSApi.requireVersion(MOD_ID, 2);

        /* Makes this addon's own assets addressable as bbslezy:... links. */
        event.registerAddon(MOD_ID, BBSLod.class);
    }

    @Subscribe
    public void onActionClips(RegisterActionClipsEvent event)
    {
        event.factory.register(Link.bbs("mob_death"), MobDeathActionClip.class, new ClipFactoryData(Icons.SKULL, Colors.RED));
        event.factory.register(Link.bbs("projectile_attack"), ProjectileAttackActionClip.class, new ClipFactoryData(Icons.ARROW_DOWN, Colors.RED));
        event.factory.register(Link.bbs("close_container"), CloseContainerActionClip.class, new ClipFactoryData(Icons.FULLSCREEN, Colors.MAGENTA));
    }
}
