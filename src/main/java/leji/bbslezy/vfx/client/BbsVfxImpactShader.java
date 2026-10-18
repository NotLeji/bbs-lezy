package leji.bbslezy.vfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import leji.bbslezy.BBSLod;
import leji.bbslezy.vfx.camera.BbsVfxImpactState;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

/**
 * The "impact frame" full-screen post pass. After BBS renders the film scene into its framebuffer, this
 * copies that frame into a scratch texture and draws it back through our {@code impact} program with the
 * effect uniforms from {@link BbsVfxImpactState}.
 */
public final class BbsVfxImpactShader
{
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("bbslezy");

    private static ShaderProgram program;
    private static Framebuffer scratch;

    /* Cached uniform references — avoids 43 String map lookups per frame on CPU */
    private static Uniform uSilhouette;
    private static Uniform uSilColor;
    private static Uniform uBgColor;
    private static Uniform uSilStrokes;
    private static Uniform uSilStrokeAngle;
    private static Uniform uSilStrokeLength;
    private static Uniform uSilStrokeScale;
    private static Uniform uSilStrokeRough;
    private static Uniform uInkBurst;
    private static Uniform uInkColor;
    private static Uniform uInkRadius;
    private static Uniform uInkInner;
    private static Uniform uInkSpikes;
    private static Uniform uInkRough;
    private static Uniform uInkSeed;
    private static Uniform uShockwave;
    private static Uniform uShockwaveProgress;
    private static Uniform uShockwaveRadius;
    private static Uniform uShockwaveWidth;
    private static Uniform uShockwaveColor;
    private static Uniform uFlashStar;
    private static Uniform uFlashStarSize;
    private static Uniform uFlashStarWidth;
    private static Uniform uFlashStarGlow;
    private static Uniform uFlashStarRotation;
    private static Uniform uFlashStarColor;
    private static Uniform uInvert;
    private static Uniform uFlash;
    private static Uniform uGrayscale;
    private static Uniform uThreshold;
    private static Uniform uThresholdLevel;
    private static Uniform uThresholdSoft;
    private static Uniform uChroma;
    private static Uniform uFlashColor;
    private static Uniform uDarkColor;
    private static Uniform uLightColor;
    private static Uniform uFocus;
    private static Uniform uZoomBlur;
    private static Uniform uBlurMode;
    private static Uniform uZoomLines;
    private static Uniform uLinesCount;
    private static Uniform uLinesThickness;
    private static Uniform uLinesInner;
    private static Uniform uLinesMode;
    private static Uniform uLinesSeed;
    private static Uniform uLinesColor;
    private static Uniform uShapes;
    private static Uniform uShapesCount;
    private static Uniform uShapesSize;
    private static Uniform uShapesSpread;
    private static Uniform uCenterStar;
    private static Uniform uCenterCircle;
    private static Uniform uShapesColor;
    private static Uniform uAspect;
    private static Uniform uBlurSamples;
    private static Uniform uShapesMax;

    private BbsVfxImpactShader()
    {}

    /** (Re)build the program on the render thread. */
    public static void build()
    {
        if (program != null)
        {
            program.close();
            program = null;
        }

        try
        {
            program = new ShaderProgram(factory(), "impact", VertexFormats.POSITION_TEXTURE);
            cacheUniforms();
        }
        catch (Exception e)
        {
            LOG.error("[impact] shader program build failed", e);
        }
    }

    private static void cacheUniforms()
    {
        if (program == null)
        {
            return;
        }

        uSilhouette = program.getUniformOrDefault("Silhouette");
        uSilColor = program.getUniformOrDefault("SilColor");
        uBgColor = program.getUniformOrDefault("BgColor");
        uSilStrokes = program.getUniformOrDefault("SilStrokes");
        uSilStrokeAngle = program.getUniformOrDefault("SilStrokeAngle");
        uSilStrokeLength = program.getUniformOrDefault("SilStrokeLength");
        uSilStrokeScale = program.getUniformOrDefault("SilStrokeScale");
        uSilStrokeRough = program.getUniformOrDefault("SilStrokeRough");
        uInkBurst = program.getUniformOrDefault("InkBurst");
        uInkColor = program.getUniformOrDefault("InkColor");
        uInkRadius = program.getUniformOrDefault("InkRadius");
        uInkInner = program.getUniformOrDefault("InkInner");
        uInkSpikes = program.getUniformOrDefault("InkSpikes");
        uInkRough = program.getUniformOrDefault("InkRough");
        uInkSeed = program.getUniformOrDefault("InkSeed");
        uShockwave = program.getUniformOrDefault("Shockwave");
        uShockwaveProgress = program.getUniformOrDefault("ShockwaveProgress");
        uShockwaveRadius = program.getUniformOrDefault("ShockwaveRadius");
        uShockwaveWidth = program.getUniformOrDefault("ShockwaveWidth");
        uShockwaveColor = program.getUniformOrDefault("ShockwaveColor");
        uFlashStar = program.getUniformOrDefault("FlashStar");
        uFlashStarSize = program.getUniformOrDefault("FlashStarSize");
        uFlashStarWidth = program.getUniformOrDefault("FlashStarWidth");
        uFlashStarGlow = program.getUniformOrDefault("FlashStarGlow");
        uFlashStarRotation = program.getUniformOrDefault("FlashStarRotation");
        uFlashStarColor = program.getUniformOrDefault("FlashStarColor");
        uInvert = program.getUniformOrDefault("Invert");
        uFlash = program.getUniformOrDefault("Flash");
        uGrayscale = program.getUniformOrDefault("Grayscale");
        uThreshold = program.getUniformOrDefault("Threshold");
        uThresholdLevel = program.getUniformOrDefault("ThresholdLevel");
        uThresholdSoft = program.getUniformOrDefault("ThresholdSoft");
        uChroma = program.getUniformOrDefault("Chroma");
        uFlashColor = program.getUniformOrDefault("FlashColor");
        uDarkColor = program.getUniformOrDefault("DarkColor");
        uLightColor = program.getUniformOrDefault("LightColor");
        uFocus = program.getUniformOrDefault("Focus");
        uZoomBlur = program.getUniformOrDefault("ZoomBlur");
        uBlurMode = program.getUniformOrDefault("BlurMode");
        uZoomLines = program.getUniformOrDefault("ZoomLines");
        uLinesCount = program.getUniformOrDefault("LinesCount");
        uLinesThickness = program.getUniformOrDefault("LinesThickness");
        uLinesInner = program.getUniformOrDefault("LinesInner");
        uLinesMode = program.getUniformOrDefault("LinesMode");
        uLinesSeed = program.getUniformOrDefault("LinesSeed");
        uLinesColor = program.getUniformOrDefault("LinesColor");
        uShapes = program.getUniformOrDefault("Shapes");
        uShapesCount = program.getUniformOrDefault("ShapesCount");
        uShapesSize = program.getUniformOrDefault("ShapesSize");
        uShapesSpread = program.getUniformOrDefault("ShapesSpread");
        uCenterStar = program.getUniformOrDefault("CenterStar");
        uCenterCircle = program.getUniformOrDefault("CenterCircle");
        uShapesColor = program.getUniformOrDefault("ShapesColor");
        uAspect = program.getUniformOrDefault("Aspect");
        uBlurSamples = program.getUniformOrDefault("uBlurSamples");
        uShapesMax = program.getUniformOrDefault("uShapesMax");
    }

    private static ResourceFactory factory()
    {
        return (id) ->
        {
            ResourceFactory manager = MinecraftClient.getInstance().getResourceManager();

            if (id.getPath().contains("/core/"))
            {
                return manager.getResource(Identifier.of(BBSLod.MOD_ID, id.getPath()));
            }

            return manager.getResource(id);
        };
    }

    private static boolean buildTried;

    /** Apply the impact effect to BBS's scene framebuffer in place. */
    public static void render(net.minecraft.client.gl.Framebuffer main)
    {
        if (program == null && !buildTried)
        {
            buildTried = true;
            build();
        }

        if (program == null || main == null || !BbsVfxImpactState.hasEffect())
        {
            return;
        }

        int w = main.textureWidth;
        int h = main.textureHeight;

        if (w <= 0 || h <= 0)
        {
            return;
        }

        Framebuffer fb = ensureScratch();
        Texture tex = fb.getMainTexture();

        if (tex.width != w || tex.height != h)
        {
            fb.resize(w, h);
        }

        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fb.id);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);

        main.beginWrite(true);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, fb.getMainTexture().id);

        int silTex = LezyImpactSilhouette.ready() ? LezyImpactSilhouette.textureId() : fb.getMainTexture().id;
        RenderSystem.setShaderTexture(1, silTex);
        RenderSystem.setShader(() -> program);

        uSilhouette.set(BbsVfxImpactState.silhouette);
        uSilColor.set(BbsVfxImpactState.silR, BbsVfxImpactState.silG, BbsVfxImpactState.silB);
        uBgColor.set(BbsVfxImpactState.bgR, BbsVfxImpactState.bgG, BbsVfxImpactState.bgB);

        uSilStrokes.set(BbsVfxImpactState.silStrokes);
        uSilStrokeAngle.set(BbsVfxImpactState.silStrokeAngle);
        uSilStrokeLength.set(BbsVfxImpactState.silStrokeLength);
        uSilStrokeScale.set(BbsVfxImpactState.silStrokeScale);
        uSilStrokeRough.set(BbsVfxImpactState.silStrokeRough);

        uInkBurst.set(BbsVfxImpactState.inkBurst);
        uInkColor.set(BbsVfxImpactState.inkR, BbsVfxImpactState.inkG, BbsVfxImpactState.inkB);
        uInkRadius.set(BbsVfxImpactState.inkRadius);
        uInkInner.set(BbsVfxImpactState.inkInner);
        uInkSpikes.set(BbsVfxImpactState.inkSpikes);
        uInkRough.set(BbsVfxImpactState.inkRough);
        uInkSeed.set(BbsVfxImpactState.inkSeed);

        uShockwave.set(BbsVfxImpactState.shockwave);
        uShockwaveProgress.set(BbsVfxImpactState.shockwaveProgress);
        uShockwaveRadius.set(BbsVfxImpactState.shockwaveRadius);
        uShockwaveWidth.set(BbsVfxImpactState.shockwaveWidth);
        uShockwaveColor.set(BbsVfxImpactState.swR, BbsVfxImpactState.swG, BbsVfxImpactState.swB);

        uFlashStar.set(BbsVfxImpactState.flashStar);
        uFlashStarSize.set(BbsVfxImpactState.flashStarSize);
        uFlashStarWidth.set(BbsVfxImpactState.flashStarWidth);
        uFlashStarGlow.set(BbsVfxImpactState.flashStarGlow);
        uFlashStarRotation.set(BbsVfxImpactState.flashStarRotation);
        uFlashStarColor.set(BbsVfxImpactState.fsR, BbsVfxImpactState.fsG, BbsVfxImpactState.fsB);

        uInvert.set(BbsVfxImpactState.invert);
        uFlash.set(BbsVfxImpactState.flash);
        uGrayscale.set(BbsVfxImpactState.grayscale);
        uThreshold.set(BbsVfxImpactState.threshold);
        uThresholdLevel.set(BbsVfxImpactState.thresholdLevel);
        uThresholdSoft.set(BbsVfxImpactState.thresholdSoft);
        uChroma.set(BbsVfxImpactState.chroma);
        uFlashColor.set(BbsVfxImpactState.flashR, BbsVfxImpactState.flashG, BbsVfxImpactState.flashB);
        uDarkColor.set(BbsVfxImpactState.darkR, BbsVfxImpactState.darkG, BbsVfxImpactState.darkB);
        uLightColor.set(BbsVfxImpactState.lightR, BbsVfxImpactState.lightG, BbsVfxImpactState.lightB);
        uFocus.set(BbsVfxImpactState.focusX, BbsVfxImpactState.focusY);

        uZoomBlur.set(BbsVfxImpactState.zoomBlur);
        uBlurMode.set(BbsVfxImpactState.blurMode);
        uZoomLines.set(BbsVfxImpactState.zoomLines);
        uLinesCount.set(BbsVfxImpactState.linesCount);
        uLinesThickness.set(BbsVfxImpactState.linesThickness);
        uLinesInner.set(BbsVfxImpactState.linesInner);
        uLinesMode.set(BbsVfxImpactState.linesMode);
        uLinesSeed.set(BbsVfxImpactState.linesSeed);
        uLinesColor.set(BbsVfxImpactState.linesR, BbsVfxImpactState.linesG, BbsVfxImpactState.linesB);
        uShapes.set(BbsVfxImpactState.shapes);
        uShapesCount.set(BbsVfxImpactState.shapesCount);
        uShapesSize.set(BbsVfxImpactState.shapesSize);
        uShapesSpread.set(BbsVfxImpactState.shapesSpread);
        uCenterStar.set(BbsVfxImpactState.centerStar);
        uCenterCircle.set(BbsVfxImpactState.centerCircle);
        uShapesColor.set(BbsVfxImpactState.shapesR, BbsVfxImpactState.shapesG, BbsVfxImpactState.shapesB);
        uAspect.set((float) w / (float) h);
        if (uBlurSamples != null) uBlurSamples.set(LezyVfxQuality.getImpactBlurSamples());
        if (uShapesMax != null) uShapesMax.set(LezyVfxQuality.getImpactShapesMax());

        BufferBuilder buffer = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        LezyVfxRenderCompat.next(buffer.vertex(-1F, -1F, 0F).texture(0F, 0F));
        LezyVfxRenderCompat.next(buffer.vertex(1F, -1F, 0F).texture(1F, 0F));
        LezyVfxRenderCompat.next(buffer.vertex(1F, 1F, 0F).texture(1F, 1F));
        LezyVfxRenderCompat.next(buffer.vertex(-1F, 1F, 0F).texture(0F, 1F));
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        GlStateManager._activeTexture(GL13.GL_TEXTURE0);

        BbsVfxImpactState.reset();
    }

    private static Framebuffer ensureScratch()
    {
        if (scratch == null)
        {
            scratch = new Framebuffer();

            Texture texture = new Texture();

            texture.setSize(2, 2);
            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            scratch.attach(texture, GL30.GL_COLOR_ATTACHMENT0);
            scratch.unbind();
        }

        return scratch;
    }
}
