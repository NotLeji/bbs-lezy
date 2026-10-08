package leji.bbslezy;

import leji.bbslezy.actions.LezyDamageActionClip;
import leji.bbslezy.client.screen.LezyFrameOverlays;
import leji.bbslezy.audio.LezyCopyAudioImporter;
import leji.bbslezy.camera.clips.screen.CinematicClip;
import leji.bbslezy.camera.clips.screen.ColorClip;
import leji.bbslezy.camera.clips.screen.LetterboxClip;
import leji.bbslezy.camera.clips.screen.VignetteClip;
import leji.bbslezy.camera.clips.screen.HalftoneClip;
import leji.bbslezy.client.screen.ScreenEffectRenderer;
import leji.bbslezy.ui.film.clips.UICinematicClip;
import leji.bbslezy.ui.film.clips.UIColorClip;
import leji.bbslezy.ui.film.clips.UILetterboxClip;
import leji.bbslezy.ui.film.clips.UIVignetteClip;
import leji.bbslezy.ui.film.clips.UIHalftoneClip;
import leji.bbslezy.forms.renderers.FormIllusionRenderer;
import leji.bbslezy.ui.forms.editors.panels.UIIllusionFormPanel;
import leji.bbslezy.ui.framework.elements.input.keyframes.factories.UIIllusionKeyframeFactory;
import leji.bbslezy.ui.framework.elements.input.keyframes.factories.UILensRadiusSettingsKeyframeFactory;
import leji.bbslezy.utils.keyframes.factories.IllusionKeyframeFactory;
import leji.bbslezy.ui.LezyIrisHelper;
import leji.bbslezy.utils.keyframes.factories.LensRadiusSettingsKeyframeFactory;
import leji.bbslezy.video.LezyEncoderProbe;
import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import leji.bbslezy.ui.skin.SkinFetcher;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.film.replays.tracks.TrackStyle;
import mchorse.bbs_mod.api.client.events.RegisterFormPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFrameOverlaysEvent;
import mchorse.bbs_mod.api.client.events.RegisterKeyframeEditorsEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.clips.actions.UIDamageActionClip;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.BBSClientReadyEvent;
import mchorse.bbs_mod.api.client.events.RegisterClientSettingsEvent;
import mchorse.bbs_mod.api.client.events.RegisterImportersEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.Collections;
import leji.bbslezy.audio.LezyAudioReverse;
import mchorse.bbs_mod.api.client.events.FilmEvents;

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
        event.register(Icons.GEAR, BBSLod.MOD_ID, (builder) ->
        {
            LodSettings.register(builder);
        });
    }

    @Subscribe
    public void onImporters(RegisterImportersEvent event)
    {
        event.register(new LezyCopyAudioImporter());
    }

    @Subscribe
    public void onClipPanels(RegisterClipPanelsEvent event)
    {
        event.register(LezyDamageActionClip.class, UIDamageActionClip::new);
        event.register(ColorClip.class, UIColorClip::new);
        event.register(LetterboxClip.class, UILetterboxClip::new);
        event.register(CinematicClip.class, UICinematicClip::new);
        event.register(VignetteClip.class, UIVignetteClip::new);
        event.register(HalftoneClip.class, UIHalftoneClip::new);
    }

    @Subscribe
    public void onFrameOverlays(RegisterFrameOverlaysEvent event)
    {
        /* BBS's own image and subtitle renderers go into this pass instead of ahead of it, so a
         * subtitle on an upper track is drawn over the effects of the tracks below it rather than
         * under them. If the registry takeover fails, LezyFrameOverlays automatically falls back
         * to drawing standard effects. */
        LezyFrameOverlays.install();

        event.register((stack, batcher, context) ->
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();

            LezyFrameOverlays.render(stack, batcher, context, w, h);
        });
    }

    @Subscribe
    public void onKeyframeEditors(RegisterKeyframeEditorsEvent event)
    {
        event.register(IllusionKeyframeFactory.INSTANCE, UIIllusionKeyframeFactory::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerIllusionPanel(mchorse.bbs_mod.ui.forms.editors.forms.UIForm uiForm)
    {
        uiForm.registerPanel(new UIIllusionFormPanel(uiForm), L10n.lang("bbslezy.ui.forms.editors.illusion"), Icons.POSE);
    }

    @Subscribe
    public void onFormPanels(RegisterFormPanelsEvent event)
    {
        event.register(BBSLodClient::registerIllusionPanel);
    }

    /**
     * The film and form render events are plain Fabric events rather than the addon bus, because
     * they run every frame — subscribe to them once, from here.
     */
    @Subscribe
    public void onClientReady(BBSClientReadyEvent event)
    {
        LodEngine.register();
        LezyEncoderProbe.startProbeAsync();
        ClientTickEvents.END_CLIENT_TICK.register((client) -> LezyIrisHelper.tick());
        UIKeyframeFactory.register(LensRadiusSettingsKeyframeFactory.INSTANCE, UILensRadiusSettingsKeyframeFactory::new);
        TrackStyle.register("illusion", Icons.POSE, Colors.DEEP_PINK);
        TrackStyle.register("illusion_transform", Icons.ALL_DIRECTIONS, 0xdd66ff);
        TrackStyle.register("bbslezy_volume", Icons.SOUND, Colors.ACTIVE);
        TrackStyle.registerLabel("bbslezy_volume", IKey.constant("Volume"));
        TrackStyle.register("volume", Icons.SOUND, Colors.ACTIVE);
        TrackStyle.registerLabel("volume", IKey.constant("Volume"));
        FormRenderEvents.AFTER.register(FormIllusionRenderer::render);
        FilmEvents.SHUTDOWN.register((film) -> LezyAudioReverse.clear());
        FilmEvents.SHUTDOWN.register((film) -> leji.bbslezy.client.screen.ColorGradeRenderer.clearTrail());
        SkinFetcher.cleanupTempFolder();
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> SkinFetcher.cleanupTempFolder());
    }
}
