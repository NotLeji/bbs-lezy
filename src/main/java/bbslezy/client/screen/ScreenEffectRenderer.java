package bbslezy.client.screen;

import bbslezy.camera.clips.screen.ColorClip;
import bbslezy.camera.clips.screen.ColorEffect;
import bbslezy.camera.clips.screen.EyeClip;
import bbslezy.camera.clips.screen.EyeEffect;
import bbslezy.camera.clips.screen.GrainClip;
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

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ScreenEffectRenderer
{
    public static void render(Batcher2D batcher, ClipContext context, int screenW, int screenH)
    {
        List<ColorEffect> effects = ColorClip.getEffects(context);
        List<LetterboxEffect> letterboxEffects = LetterboxClip.getEffects(context);
        List<GrainEffect> grainEffects = GrainClip.getEffects(context);
        List<EyeEffect> eyeEffects = EyeClip.getEffects(context);

        if (effects.isEmpty() && letterboxEffects.isEmpty() && grainEffects.isEmpty() && eyeEffects.isEmpty())
        {
            return;
        }

        int[] prevViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        RenderSystem.disableDepthTest();

        int effectIndex = 0;
        int letterboxIndex = 0;
        int grainIndex = 0;
        int eyeIndex = 0;

        List<ColorEffect> pendingShaderEffects = new ArrayList<>();
        List<GrainEffect> pendingGrainEffects = new ArrayList<>();

        while (effectIndex < effects.size()
            || letterboxIndex < letterboxEffects.size()
            || grainIndex < grainEffects.size()
            || eyeIndex < eyeEffects.size())
        {
            int effOrder = effectIndex < effects.size() ? effects.get(effectIndex).renderOrder : Integer.MAX_VALUE;
            int letOrder = letterboxIndex < letterboxEffects.size() ? letterboxEffects.get(letterboxIndex).renderOrder : Integer.MAX_VALUE;
            int grnOrder = grainIndex < grainEffects.size() ? grainEffects.get(grainIndex).renderOrder : Integer.MAX_VALUE;
            int eyeOrder = eyeIndex < eyeEffects.size() ? eyeEffects.get(eyeIndex).renderOrder : Integer.MAX_VALUE;

            int nextOrder = Math.min(Math.min(effOrder, letOrder), Math.min(grnOrder, eyeOrder));

            boolean hasDirectDraw = (eyeOrder == nextOrder)
                || (letOrder == nextOrder)
                || (effOrder == nextOrder && effects.get(effectIndex).hasOverlay);

            if (hasDirectDraw && (!pendingShaderEffects.isEmpty() || !pendingGrainEffects.isEmpty()))
            {
                batcher.flush();
                ColorGradeRenderer.apply(pendingShaderEffects, pendingGrainEffects);
                ColorGradeRenderer.resyncMinecraftState(batcher);
                pendingShaderEffects.clear();
                pendingGrainEffects.clear();
            }

            if (eyeOrder == nextOrder)
            {
                renderEye(batcher, eyeEffects.get(eyeIndex), screenW, screenH);
                eyeIndex += 1;
            }
            else if (effOrder == nextOrder)
            {
                ColorEffect effect = effects.get(effectIndex);

                if (effect.hasOverlay)
                {
                    batcher.box(0, 0, screenW, screenH, effect.overlayColor);
                }

                if (effect.hasGrade || effect.hasVignette || effect.hasDistort || effect.hasCinematic)
                {
                    pendingShaderEffects.add(effect);
                }

                effectIndex += 1;
            }
            else if (grnOrder == nextOrder)
            {
                GrainEffect grain = grainEffects.get(grainIndex);

                if (grain.strength > 0F)
                {
                    pendingGrainEffects.add(grain);
                }

                grainIndex += 1;
            }
            else if (letOrder == nextOrder)
            {
                renderLetterbox(batcher, letterboxEffects.get(letterboxIndex), screenW, screenH);
                letterboxIndex += 1;
            }
        }

        if (!pendingShaderEffects.isEmpty() || !pendingGrainEffects.isEmpty())
        {
            batcher.flush();
            ColorGradeRenderer.apply(pendingShaderEffects, pendingGrainEffects);
            ColorGradeRenderer.resyncMinecraftState(batcher);
            pendingShaderEffects.clear();
            pendingGrainEffects.clear();
        }

        effects.clear();
        letterboxEffects.clear();
        grainEffects.clear();
        eyeEffects.clear();

        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        RenderSystem.enableDepthTest();
    }

    private static void renderLetterbox(Batcher2D batcher, LetterboxEffect effect, int screenW, int screenH)
    {
        if (effect.width <= 0F)
        {
            return;
        }

        int barH = (int)(screenH * effect.size);

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
        int smoothH = (int)(screenH * effect.smoothness);
        float barWidthFactor = effect.width;
        int barW = Math.max(1, Math.round(screenW * barWidthFactor));
        int barX = (screenW - barW) / 2;

        if (smoothH > 0 && smoothH < barH)
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

    private static void renderEye(Batcher2D batcher, EyeEffect effect, int screenW, int screenH)
    {
        float blink = Math.min(1F, effect.size / 0.025F);

        if (blink <= 0.001F)
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
            renderEyeMask(batcher, effect, screenW, screenH, blink);
            stack.pop();
        }
        else
        {
            renderEyeMask(batcher, effect, screenW, screenH, blink);
        }
    }

    private static void renderEyeMask(Batcher2D batcher, EyeEffect effect, int screenW, int screenH, float blink)
    {
        int color = effect.color;
        int alphaColor = Colors.setA(color, Colors.getA(color) * blink);
        int transparent = Colors.setA(color, 0F);
        float widthFactor = effect.width <= 0F ? 1F : effect.width;
        float open = Math.max(0F, 1F - effect.size);
        float halfW = screenW / 2F * widthFactor;
        float halfH = screenH / 2F * open;
        float centerX = screenW / 2F;
        float centerY = screenH / 2F;

        if (open > 0.3F && widthFactor > 0F)
        {
            double aspect = Math.sqrt(1D + Math.pow(screenW / (double) screenH / widthFactor, 2D));
            float eased = Math.min(1F, (open - 0.3F) / 0.7F);
            float scale = 1F + ((float) aspect - 1F) * eased;

            halfW *= scale;
            halfH *= scale;
        }

        if (halfH <= 0F)
        {
            batcher.box(0F, 0F, screenW, screenH, alphaColor);

            return;
        }

        float inner = 1F - MathUtils.clamp(effect.smoothness, 0F, 1F);
        float halfWIn = halfW * inner;
        float halfHIn = halfH * inner;
        boolean feather = inner < 1F;
        int spanStart = -1;

        for (int y = 0; y <= screenH; y++)
        {
            if (y < screenH && Math.abs((y + 0.5F) - centerY) / halfH >= 1F)
            {
                if (spanStart < 0)
                {
                    spanStart = y;
                }

                continue;
            }

            if (spanStart >= 0)
            {
                batcher.box(0F, spanStart, screenW, y, alphaColor);
                spanStart = -1;
            }

            if (y >= screenH)
            {
                break;
            }

            float dy = Math.abs((y + 0.5F) - centerY);
            float vy = dy / halfH;
            float halfEllipseW = halfW * (float) Math.sqrt(Math.max(0D, 1D - (double) vy * vy));
            int ellLeft = (int) Math.ceil(centerX - halfEllipseW);
            int ellRight = (int) Math.floor(centerX + halfEllipseW);

            if (feather)
            {
                float halfInnerW = 0F;
                int innerColor = transparent;

                if (halfHIn > 0F && dy < halfHIn)
                {
                    float viy = dy / halfHIn;

                    halfInnerW = halfWIn * (float) Math.sqrt(Math.max(0D, 1D - (double) viy * viy));
                }
                else
                {
                    float t = MathUtils.clamp((vy - inner) / (1F - inner), 0F, 1F);

                    innerColor = Colors.setA(color, Colors.getA(color) * blink * t);
                }

                int inLeft = (int) Math.ceil(centerX - halfInnerW);
                int inRight = (int) Math.floor(centerX + halfInnerW);

                if (ellLeft > 0)
                {
                    batcher.box(0F, y, ellLeft, y + 1, alphaColor);
                }

                if (inLeft > ellLeft)
                {
                    batcher.gradientHBox(ellLeft, y, inLeft, y + 1, alphaColor, innerColor);
                }

                if (ellRight < screenW)
                {
                    batcher.box(ellRight, y, screenW, y + 1, alphaColor);
                }

                if (inRight < ellRight)
                {
                    batcher.gradientHBox(inRight, y, ellRight, y + 1, innerColor, alphaColor);
                }
            }
            else
            {
                if (ellLeft > 0)
                {
                    batcher.box(0F, y, ellLeft, y + 1, alphaColor);
                }

                if (ellRight < screenW)
                {
                    batcher.box(ellRight, y, screenW, y + 1, alphaColor);
                }
            }
        }
    }
}
