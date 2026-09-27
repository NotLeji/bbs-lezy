package bbslezy.client.screen;

import bbslezy.camera.clips.screen.CinematicClip;
import bbslezy.camera.clips.screen.ColorClip;
import bbslezy.camera.clips.screen.ColorEffect;
import bbslezy.camera.clips.screen.GrainEffect;
import bbslezy.camera.clips.screen.LetterboxClip;
import bbslezy.camera.clips.screen.LetterboxEffect;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ScreenEffectRenderer
{
    public static void render(Batcher2D batcher, ClipContext context, int screenW, int screenH)
    {
        List<ColorEffect> effects = ColorClip.getEffects(context);
        List<LetterboxEffect> letterboxEffects = LetterboxClip.getEffects(context);
        List<GrainEffect> grainEffects = CinematicClip.getGrainEffects(context);

        if (effects.isEmpty() && letterboxEffects.isEmpty() && grainEffects.isEmpty())
        {
            return;
        }

        int[] prevViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        RenderSystem.disableDepthTest();

        List<ColorEffect> shaderEffects = new ArrayList<>();

        for (ColorEffect effect : effects)
        {
            if (effect.hasOverlay)
            {
                batcher.box(0, 0, screenW, screenH, effect.overlayColor);
            }

            if (effect.hasGrade || effect.hasVignette || effect.hasDistort || effect.hasCinematic)
            {
                shaderEffects.add(effect);
            }
        }

        /* 1. Jalankan shader post-processing (vintage flicker, scratches, color grade, fisheye, grain, dll) */
        if (!shaderEffects.isEmpty() || !grainEffects.isEmpty())
        {
            batcher.flush();
            ColorGradeRenderer.apply(shaderEffects, grainEffects);
            ColorGradeRenderer.resyncMinecraftState(batcher);
        }

        /* 2. Gambar letterbox bars di atas frame dengan projection matrix ortho yang tepat */
        if (!letterboxEffects.isEmpty())
        {
            Matrix4f cache = new Matrix4f(RenderSystem.getProjectionMatrix());
            Matrix4f ortho = new Matrix4f().ortho(0, screenW, screenH, 0, -1000, 3000);
            RenderSystem.setProjectionMatrix(ortho, VertexSorter.BY_Z);

            for (LetterboxEffect le : letterboxEffects)
            {
                renderLetterbox(batcher, le, screenW, screenH);
            }

            batcher.flush();
            RenderSystem.setProjectionMatrix(cache, VertexSorter.BY_Z);
        }

        effects.clear();
        letterboxEffects.clear();
        grainEffects.clear();

        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        RenderSystem.enableDepthTest();
    }

    private static void renderLetterbox(Batcher2D batcher, LetterboxEffect effect, int screenW, int screenH)
    {
        if (effect.width <= 0F)
        {
            return;
        }

        int barH = (int) (screenH * effect.size);

        if (barH <= 0)
        {
            return;
        }

        float zoom = effect.zoom <= 0F ? 1F : effect.zoom;
        boolean transformed = effect.rotation != 0F || zoom != 1F || effect.offsetX != 0F || effect.offsetY != 0F;

        if (transformed)
        {
            MatrixStack stack = batcher.getContext().getMatrices();

            stack.push();
            stack.translate(effect.offsetX * screenW, effect.offsetY * screenH, 0F);
            stack.translate(screenW / 2F, screenH / 2F, 0F);
            stack.multiply(RotationAxis.POSITIVE_Z.rotation(MathUtils.toRad(effect.rotation)));
            stack.scale(zoom, zoom, 1F);
            stack.translate(-screenW / 2F, -screenH / 2F, 0F);
            renderLetterboxBars(batcher, effect, screenW, screenH, barH);
            stack.pop();
        }
        else
        {
            renderLetterboxBars(batcher, effect, screenW, screenH, barH);
        }
    }

    private static void renderLetterboxBars(Batcher2D batcher, LetterboxEffect effect, int screenW, int screenH, int barH)
    {
        int color = effect.color;
        int smoothH = (int) (barH * MathUtils.clamp(effect.smoothness, 0F, 1F));
        float barWidthFactor = effect.width;
        int barW = Math.max(1, Math.round(screenW * barWidthFactor));
        int barX = (screenW - barW) / 2;

        if (smoothH > 0)
        {
            int solidH = barH - smoothH;
            int transparent = Colors.setA(color, 0F);

            batcher.box(barX, 0, barX + barW, solidH, color);
            batcher.gradientVBox(barX, solidH, barX + barW, barH, color, transparent);

            batcher.gradientVBox(barX, screenH - barH, barX + barW, screenH - solidH, transparent, color);
            batcher.box(barX, screenH - solidH, barX + barW, screenH, color);
        }
        else
        {
            batcher.box(barX, 0, barX + barW, barH, color);
            batcher.box(barX, screenH - barH, barX + barW, screenH, color);
        }
    }
}
