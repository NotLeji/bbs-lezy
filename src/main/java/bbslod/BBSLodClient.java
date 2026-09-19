package bbslod;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.BBSClientReadyEvent;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackStylesEvent;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * The client half of the LOD addon, registered under {@code bbs-client-addon}.
 */
public class BBSLodClient implements BBSAddonMod
{
    @Subscribe
    public void onClientSettings(RegisterClientSettingsEvent event)
    {
        event.register(Icons.GEAR, BBSLod.MOD_ID, LodSettings::register);
    }

    /** Gives this addon's two values their own colour and icon on the timeline. */
    @Subscribe
    public void onTrackStyles(RegisterTrackStylesEvent event)
    {
        event.register(BBSLod.CULL, Icons.CURVES, Colors.CYAN);
        event.register(BBSLod.SIMPLIFY, Icons.CURVES, Colors.CYAN);
    }

    /**
     * The film and form render events are plain Fabric events rather than the addon bus, because
     * they run every frame — subscribe to them once, from here.
     */
    @Subscribe
    public void onClientReady(BBSClientReadyEvent event)
    {
        LodEngine.register();
    }
}
