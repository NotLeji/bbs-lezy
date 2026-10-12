package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import org.joml.Vector3f;
import leji.bbslezy.vfx.forms.CurveForm;
import leji.bbslezy.vfx.forms.CurvePoint;

import java.util.List;

/**
 * Draws a {@link CurveForm}'s actual 3D geometry into the form-picker / Recent Forms thumbnail box.
 */
public final class CurveFormThumbnailRenderer
{
    private static final UIFormRenderer RENDERER = new UIFormRenderer();

    static
    {
        RENDERER.grid = false;
    }

    private CurveFormThumbnailRenderer()
    {}

    public static void render(UIContext context, CurveForm curve, int x, int y, int width, int height)
    {
        if (context == null || curve == null || width <= 0 || height <= 0)
        {
            return;
        }

        synchronized (RENDERER)
        {
            RENDERER.form = curve;
            RENDERER.resetFlex().x(x).y(y).w(width).h(height);
            RENDERER.resize();

            setupCamera(curve);
            RENDERER.render(context);
            RENDERER.form = null;
        }
    }

    private static void setupCamera(CurveForm curve)
    {
        List<CurvePoint> points = curve.points.getAllTyped();

        if (points.isEmpty())
        {
            RENDERER.setPosition(0F, 0F, 0F);
            RENDERER.setDistance(22);
            RENDERER.setRotation(35F, -25F);

            return;
        }

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        for (CurvePoint point : points)
        {
            Vector3f p = curve.pointPosition(point);

            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }

        float cx = (minX + maxX) * 0.5F;
        float cy = (minY + maxY) * 0.5F;
        float cz = (minZ + maxZ) * 0.5F;

        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        float distance = Math.max(3F, extent * 2.2F);

        RENDERER.setPosition(-cx, -cy, -cz);
        RENDERER.setDistance(distance);
        RENDERER.setRotation(35F, -25F);
    }
}
