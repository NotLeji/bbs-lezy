package bbslezy.ui;

import bbslod.LodSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public class UILodContextMenu extends UIContextMenu
{
    private final UIElement column;

    public UILodContextMenu()
    {
        UIToggle enabled = new UIToggle(L10n.lang("bbslezy.ui.lod.enabled"), (b) ->
        {
            LodSettings.enabled.set(b.getValue());
        });
        enabled.setValue(LodSettings.enabled.get());

        UISliderTrackpad limit = new UISliderTrackpad((v) ->
        {
            LodSettings.renderLimit.set(v.intValue());
        });
        limit.limit(LodSettings.renderLimit).setValue(LodSettings.renderLimit.get());

        UISliderTrackpad focus = new UISliderTrackpad((v) ->
        {
            LodSettings.focusDistance.set(v.doubleValue());
        });
        focus.limit(LodSettings.focusDistance).increment(5D).values(1D, 0.5D, 5D).setValue(LodSettings.focusDistance.get());

        this.column = UI.column(5, 8,
            enabled,
            UI.labelRow(L10n.lang("bbslezy.ui.lod.render_limit"), 80, limit),
            UI.labelRow(L10n.lang("bbslezy.ui.lod.focus_distance"), 80, focus)
        );
        this.column.relative(this).w(220);

        this.add(this.column);
        this.column.resize();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.xy(context.mouseX(), context.mouseY())
            .wh(this.column.area.w, this.column.area.h)
            .bounds(context.menu.overlay, 5);
    }
}
