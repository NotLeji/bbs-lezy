package leji.bbslezy;

import leji.bbslezy.actions.LezyDamageActionClip;
import leji.bbslezy.client.screen.LezyFrameOverlays;
import leji.bbslezy.audio.LezyCopyAudioImporter;
import leji.bbslezy.camera.clips.screen.CinematicClip;
import leji.bbslezy.camera.clips.screen.ColorClip;
import leji.bbslezy.camera.clips.screen.LetterboxClip;
import leji.bbslezy.camera.clips.screen.VignetteClip;
import leji.bbslezy.camera.clips.screen.HalftoneClip;
import leji.bbslezy.camera.clips.modifiers.ProceduralShakeClip;
import leji.bbslezy.camera.clips.screen.ImpactClip;
import leji.bbslezy.client.screen.ScreenEffectRenderer;
import leji.bbslezy.vfx.client.BeamFormRenderer;
import leji.bbslezy.vfx.client.CurveFormRenderer;
import leji.bbslezy.vfx.client.FollowCurveClientClip;
import leji.bbslezy.vfx.client.UICurveForm;
import leji.bbslezy.vfx.client.UIFollowCurveClip;
import leji.bbslezy.vfx.forms.CurveForm;
import leji.bbslezy.vfx.client.BeamHeat;
import leji.bbslezy.vfx.client.BeamVolume;
import leji.bbslezy.vfx.client.CracksVolume;
import leji.bbslezy.vfx.client.DestructionShader;
import leji.bbslezy.vfx.client.DomeFormRenderer;
import leji.bbslezy.vfx.client.DomeVolume;
import leji.bbslezy.vfx.client.ExplosionSmokeVolume;
import leji.bbslezy.vfx.client.LezyVfxExplosionFx;
import leji.bbslezy.vfx.client.LezyVfxIcons;
import leji.bbslezy.vfx.client.SmokeVolume;
import leji.bbslezy.vfx.client.UIBeamForm;
import leji.bbslezy.vfx.client.UIDestructionKeyframeFactory;
import leji.bbslezy.vfx.client.UIDomeForm;
import leji.bbslezy.vfx.client.LezyWindFoliage;
import leji.bbslezy.vfx.client.LezyWindFormRenderer;
import leji.bbslezy.vfx.client.LezyWindVolume;
import leji.bbslezy.vfx.client.UIWindForm;
import leji.bbslezy.vfx.forms.LezyWindForm;
import mchorse.bbs_mod.api.client.editor.TrackCategory;
import mchorse.bbs_mod.api.client.events.RegisterTrackCategoriesEvent;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import leji.bbslezy.vfx.client.VfxFormSection;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import leji.bbslezy.vfx.DestructionSelection;
import leji.bbslezy.vfx.client.DestructionBoxFormRenderer;
import leji.bbslezy.vfx.client.UIDestructionBoxForm;
import leji.bbslezy.vfx.client.UIExplosionForm;
import leji.bbslezy.vfx.forms.BeamForm;
import leji.bbslezy.vfx.forms.DestructionBoxForm;
import leji.bbslezy.vfx.forms.DomeForm;
import leji.bbslezy.vfx.forms.ExplosionForm;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import leji.bbslezy.ui.film.clips.UICinematicClip;
import leji.bbslezy.ui.film.clips.UIColorClip;
import leji.bbslezy.ui.film.clips.UILetterboxClip;
import leji.bbslezy.ui.film.clips.UIVignetteClip;
import leji.bbslezy.ui.film.clips.UIHalftoneClip;
import leji.bbslezy.ui.film.clips.UIProceduralShakeClip;
import leji.bbslezy.ui.film.clips.UIImpactClip;
import leji.bbslezy.ui.film.UIGuidesOverlay;
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
import mchorse.bbs_mod.api.client.events.RegisterPreviewOverlaysEvent;
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
        event.register(ProceduralShakeClip.class, UIProceduralShakeClip::new);
        event.register(ImpactClip.class, UIImpactClip::new);
        event.register(FollowCurveClientClip.class, UIFollowCurveClip::new);
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
    public void onPreviewOverlays(RegisterPreviewOverlaysEvent event)
    {
        event.register(UIGuidesOverlay::new);
    }

    @Subscribe
    public void onKeyframeEditors(RegisterKeyframeEditorsEvent event)
    {
        event.register(IllusionKeyframeFactory.INSTANCE, UIIllusionKeyframeFactory::new);
        event.registerProperty("destruction", UIDestructionKeyframeFactory::new);
        event.registerProperty("progress", UIDestructionKeyframeFactory::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerIllusionPanel(mchorse.bbs_mod.ui.forms.editors.forms.UIForm uiForm)
    {
        uiForm.registerPanel(new UIIllusionFormPanel(uiForm), L10n.lang("bbslezy.ui.forms.editors.illusion"), Icons.POSE);
    }

    @Subscribe
    public void onTrackCategories(RegisterTrackCategoriesEvent event)
    {
        TrackCategory vfx = new TrackCategory("bbslezy:vfx", LezyVfxIcons.EXPLOSION,
            () -> L10n.lang("bbslezy.ui.film.replays.category.vfx").get(),
            () -> L10n.lang("bbslezy.ui.film.replays.category.vfx.tooltip").get());

        event.register(vfx, (trackId, owned) ->
        {
            if (trackId == null) return false;
            String p = trackId.subject();
            if (p.isEmpty()) p = trackId.property();
            return "destruction".equals(p) || "progress".equals(p) || p.startsWith("phys_") || p.startsWith("wind_");
        });
    }

    @Subscribe
    public void onFormPanels(RegisterFormPanelsEvent event)
    {
        event.register(BBSLodClient::registerIllusionPanel);
    }

    @Subscribe
    public void onFormSections(RegisterFormSectionsEvent event)
    {
        event.register(VfxFormSection::new);
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
        FormUtilsClient.register(DestructionBoxForm.class, DestructionBoxFormRenderer::new);
        FormUtilsClient.register((Class) ExplosionForm.class,
            (FormUtilsClient.IFormRendererFactory) form -> new DestructionBoxFormRenderer((DestructionBoxForm) form));
        FormUtilsClient.register(BeamForm.class, BeamFormRenderer::new);
        FormUtilsClient.register(DomeForm.class, DomeFormRenderer::new);
        FormUtilsClient.register(LezyWindForm.class, LezyWindFormRenderer::new);
        FormUtilsClient.register(CurveForm.class, CurveFormRenderer::new);
        UIFormEditor.register(CurveForm.class, UICurveForm::new);
        UIFormEditor.register(DestructionBoxForm.class, UIDestructionBoxForm::new);
        UIFormEditor.register(ExplosionForm.class, UIExplosionForm::new);
        UIFormEditor.register(BeamForm.class, UIBeamForm::new);
        UIFormEditor.register(DomeForm.class, UIDomeForm::new);
        UIFormEditor.register(LezyWindForm.class, UIWindForm::new);
        TrackStyle.register("destruction", LezyVfxIcons.DESTRUCTION, Colors.NEGATIVE);
        TrackStyle.registerLabel("destruction", IKey.constant("Destruction"));
        TrackStyle.register("progress", LezyVfxIcons.DOME, Colors.POSITIVE);
        TrackStyle.registerLabel("progress", IKey.constant("Progress"));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "destruction_box"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                prog -> DestructionShader.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "destruction_ballistic"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                prog -> DestructionShader.BALLISTIC = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "cracks_volume"),
                VertexFormats.POSITION_TEXTURE,
                prog -> CracksVolume.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "smoke_volume"),
                VertexFormats.POSITION_TEXTURE,
                prog -> SmokeVolume.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "explosion_smoke"),
                VertexFormats.POSITION_TEXTURE,
                prog -> ExplosionSmokeVolume.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "beam_heat"),
                VertexFormats.POSITION_COLOR_TEXTURE,
                prog -> BeamHeat.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "beam_volume"),
                VertexFormats.POSITION_TEXTURE,
                prog -> BeamVolume.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "dome_volume"),
                VertexFormats.POSITION_TEXTURE,
                prog -> DomeVolume.PROGRAM = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "dome_level_inst"),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                prog -> DestructionShader.DOME = prog));
        CoreShaderRegistrationCallback.EVENT.register(ctx ->
            ctx.register(new Identifier(BBSLod.MOD_ID, "wind_volume"),
                VertexFormats.POSITION_TEXTURE,
                prog -> LezyWindVolume.PROGRAM = prog));

        WorldRenderEvents.LAST.register(LezyVfxExplosionFx::drawDeferred);
        WorldRenderEvents.LAST.register(BeamHeat::captureDepth);
        WorldRenderEvents.LAST.register(BeamVolume::captureDepth);
        WorldRenderEvents.LAST.register(DomeVolume::captureDepth);
        WorldRenderEvents.LAST.register(CracksVolume::captureDepth);
        WorldRenderEvents.LAST.register(SmokeVolume::captureDepth);
        WorldRenderEvents.LAST.register(ExplosionSmokeVolume::captureDepth);
        WorldRenderEvents.LAST.register(LezyWindVolume::captureDepth);
        WorldRenderEvents.LAST.register(context -> LezyWindFoliage.tick());
        FormRenderEvents.AFTER.register(FormIllusionRenderer::render);
        FilmEvents.SHUTDOWN.register((film) -> LezyAudioReverse.clear());
        FilmEvents.SHUTDOWN.register((film) -> leji.bbslezy.client.screen.ColorGradeRenderer.clearTrail());
        SkinFetcher.cleanupTempFolder();
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> SkinFetcher.cleanupTempFolder());
    }
}
