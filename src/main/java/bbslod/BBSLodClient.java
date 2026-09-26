package bbslod;

import bbslezy.actions.MobDeathActionClip;
import bbslezy.actions.ProjectileAttackActionClip;
import bbslezy.audio.LezyCopyAudioImporter;
import bbslezy.ui.film.clips.actions.UIMobDeathActionClip;
import bbslezy.client.screen.ScreenEffectRenderer;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFrameOverlaysEvent;
import net.minecraft.client.MinecraftClient;
import mchorse.bbs_mod.ui.film.clips.actions.UIAttackActionClip;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.BBSClientReadyEvent;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterImportersEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.Collections;

/**
 * The client half of the BBS Lezy addon, registered under {@code bbs-client-addon}.
 */
public class BBSLodClient implements BBSAddonMod
{
    /**
     * Supplies the settings screen's labels from the addon's own strings file, so the two
     * settings read as what they do instead of as their raw ids.
     */
    @Subscribe
    public void onL10n(RegisterL10nEvent event)
    {
        event.l10n.register((lang) -> Collections.singletonList(Link.create(BBSLod.MOD_ID + ":strings/" + lang + ".json")));
    }

    @Subscribe
    public void onClientSettings(RegisterClientSettingsEvent event)
    {
        event.register(Icons.GEAR, BBSLod.MOD_ID, LodSettings::register);
    }

    @Subscribe
    public void onImporters(RegisterImportersEvent event)
    {
        event.register(new LezyCopyAudioImporter());
    }

    @Subscribe
    public void onClipPanels(RegisterClipPanelsEvent event)
    {
        event.register(MobDeathActionClip.class, UIMobDeathActionClip::new);
        event.register(ProjectileAttackActionClip.class, UIAttackActionClip::new);
    }

    @Subscribe
    public void onFrameOverlays(RegisterFrameOverlaysEvent event)
    {
        event.register((stack, batcher, context) ->
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();

            ScreenEffectRenderer.render(batcher, context, w, h);
        });
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
