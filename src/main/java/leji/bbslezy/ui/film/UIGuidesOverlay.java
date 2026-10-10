package leji.bbslezy.ui.film;

import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

public class UIGuidesOverlay extends UIElement
{
    private final UIFilmPreview preview;
    private final UIIcon guidesBtn;

    private boolean savedActionSafe;
    private boolean savedTitleSafe;
    private boolean savedVertical;
    private boolean savedCinematic;
    private boolean savedCenterLines;
    private boolean savedCrosshair;

    public UIGuidesOverlay(UIFilmPreview preview)
    {
        this.preview = preview;
        this.full(preview);
        this.guidesBtn = new UIIcon(Icons.CROPS, (b) -> this.toggleAllGuides());
        this.guidesBtn.context(this::createGuidesMenu);
        this.guidesBtn.tooltip(IKey.raw("Framing Guides / Safe Areas (Left-click: toggle, Right-click: options)"), Direction.BOTTOM);
        this.guidesBtn.highlight(this::hasAnyActiveGuide, Direction.BOTTOM);

        preview.icons.add(this.guidesBtn);
    }

    public boolean hasAnyActiveGuide()
    {
        return LodSettings.guidesActionSafe.get()
            || LodSettings.guidesTitleSafe.get()
            || LodSettings.guidesVertical.get()
            || LodSettings.guidesCinematic.get()
            || BBSSettings.editorCenterLines.get()
            || BBSSettings.editorCrosshair.get();
    }

    private void toggleAllGuides()
    {
        if (this.hasAnyActiveGuide())
        {
            this.savedActionSafe = LodSettings.guidesActionSafe.get();
            this.savedTitleSafe = LodSettings.guidesTitleSafe.get();
            this.savedVertical = LodSettings.guidesVertical.get();
            this.savedCinematic = LodSettings.guidesCinematic.get();
            this.savedCenterLines = BBSSettings.editorCenterLines.get();
            this.savedCrosshair = BBSSettings.editorCrosshair.get();

            LodSettings.guidesActionSafe.set(false);
            LodSettings.guidesTitleSafe.set(false);
            LodSettings.guidesVertical.set(false);
            LodSettings.guidesCinematic.set(false);
            BBSSettings.editorCenterLines.set(false);
            BBSSettings.editorCrosshair.set(false);
        }
        else
        {
            boolean anySaved = this.savedActionSafe || this.savedTitleSafe || this.savedVertical
                || this.savedCinematic || this.savedCenterLines || this.savedCrosshair;

            if (anySaved)
            {
                LodSettings.guidesActionSafe.set(this.savedActionSafe);
                LodSettings.guidesTitleSafe.set(this.savedTitleSafe);
                LodSettings.guidesVertical.set(this.savedVertical);
                LodSettings.guidesCinematic.set(this.savedCinematic);
                BBSSettings.editorCenterLines.set(this.savedCenterLines);
                BBSSettings.editorCrosshair.set(this.savedCrosshair);
            }
            else
            {
                BBSSettings.editorCenterLines.set(true);
            }
        }
    }

    private void createGuidesMenu(ContextMenuManager menu)
    {
        menu.action(Icons.CROPS, IKey.raw("Center Lines"), BBSSettings.editorCenterLines.get(), () ->
        {
            BBSSettings.editorCenterLines.set(!BBSSettings.editorCenterLines.get());
        });
        menu.action(Icons.CROPS, IKey.raw("Crosshair"), BBSSettings.editorCrosshair.get(), () ->
        {
            BBSSettings.editorCrosshair.set(!BBSSettings.editorCrosshair.get());
        });
        menu.action(Icons.CROPS, IKey.raw("Action Safe (90%)"), LodSettings.guidesActionSafe.get(), () ->
        {
            LodSettings.guidesActionSafe.set(!LodSettings.guidesActionSafe.get());
        });
        menu.action(Icons.CROPS, IKey.raw("Title Safe (80%)"), LodSettings.guidesTitleSafe.get(), () ->
        {
            LodSettings.guidesTitleSafe.set(!LodSettings.guidesTitleSafe.get());
        });
        menu.action(Icons.CROPS, IKey.raw("9:16 Vertical"), LodSettings.guidesVertical.get(), () ->
        {
            LodSettings.guidesVertical.set(!LodSettings.guidesVertical.get());
        });
        menu.action(Icons.CROPS, IKey.raw("2.39:1 Scope"), LodSettings.guidesCinematic.get(), () ->
        {
            LodSettings.guidesCinematic.set(!LodSettings.guidesCinematic.get());
        });
    }


    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.hasAnyActiveGuide())
        {
            return;
        }

        Area vp = this.preview.getViewport();
        if (vp == null || vp.w <= 0 || vp.h <= 0)
        {
            return;
        }

        int color = LodSettings.guidesColor.get();
        boolean showLabels = LodSettings.guidesShowLabels.get();
        Batcher2D batcher = context.batcher;

        /* Action Safe: 90% size (5% inset on all sides) */
        if (LodSettings.guidesActionSafe.get())
        {
            float asX = vp.x + vp.w * 0.05F;
            float asY = vp.y + vp.h * 0.05F;
            float asW = vp.w * 0.90F;
            float asH = vp.h * 0.90F;

            batcher.outline(asX, asY, asX + asW, asY + asH, color);
            if (showLabels)
            {
                batcher.text("Action Safe 90%", asX + 4, asY + 4, color);
            }
        }

        /* Title Safe: 80% size (10% inset on all sides) */
        if (LodSettings.guidesTitleSafe.get())
        {
            float tsX = vp.x + vp.w * 0.10F;
            float tsY = vp.y + vp.h * 0.10F;
            float tsW = vp.w * 0.80F;
            float tsH = vp.h * 0.80F;

            batcher.outline(tsX, tsY, tsX + tsW, tsY + tsH, color);
            if (showLabels)
            {
                batcher.text("Title Safe 80%", tsX + 4, tsY + 4, color);
            }
        }

        /* 9:16 Vertical guide (centered) */
        if (LodSettings.guidesVertical.get())
        {
            float vertW = vp.h * (9F / 16F);
            if (vertW < vp.w)
            {
                float vertX = vp.mx() - vertW / 2F;
                batcher.outline(vertX, vp.y, vertX + vertW, vp.ey(), color);
                if (showLabels)
                {
                    batcher.text("9:16", vertX + 4, vp.y + 4, color);
                }
            }
        }

        /* 2.39:1 Cinema Scope guide (centered) */
        if (LodSettings.guidesCinematic.get())
        {
            float scopeH = vp.w / 2.39F;
            if (scopeH < vp.h)
            {
                float scopeY = vp.my() - scopeH / 2F;
                batcher.outline(vp.x, scopeY, vp.ex(), scopeY + scopeH, color);
                if (showLabels)
                {
                    batcher.text("2.39:1", vp.x + 4, scopeY + 4, color);
                }
            }
        }
    }
}
