package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;


/**
 * Small UI helper for the addon: builds a BBS {@link LezyVfxSection} (the collapsible header-card BBS 2.3.1
 * uses to group panel controls) from a title and its fields, so all addon feature panels group their
 * controls the same idiomatic way as stock BBS panels.
 */
public final class LezyVfxUI
{
    private LezyVfxUI()
    {
    }

    /** A collapsible section titled {@code title} containing {@code fields}. */
    public static LezyVfxSection section(IKey title, IUIElement... fields)
    {
        LezyVfxSection section = new LezyVfxSection(title);

        section.fields.add(fields);

        return section;
    }

    /** Convenience overload taking a plain string title. */
    public static LezyVfxSection section(String title, IUIElement... fields)
    {
        return section(IKey.constant(title), fields);
    }
}
