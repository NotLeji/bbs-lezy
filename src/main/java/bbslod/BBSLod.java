package bbslod;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.BBSApi;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterFormModifiersEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

/**
 * The common half of the LOD addon — registered under the {@code bbs-addon} entry point and loaded
 * on both sides.
 *
 * <p>Its client half is {@link BBSLodClient}, and it has to be a separate class: the client events
 * live in BBS's client source set, and a class that so much as mentions one of them cannot be
 * loaded on a dedicated server.</p>
 */
public class BBSLod implements BBSAddonMod
{
    public static final String MOD_ID = "bbslod";

    /**
     * Per-form override for the distance at which the form is hidden entirely. {@code -1} means
     * "inherit the global setting"; any value {@code >= 0} overrides for this form.
     *
     * <p>Namespaced on purpose. That is what makes BBS keep it in the saved data while this addon
     * is not loaded — an un-namespaced key is indistinguishable from one of BBS's own that was
     * removed, and is dropped on the next save.</p>
     */
    public static final String CULL = MOD_ID + ":cull_distance";

    /** Same contract as {@link #CULL}, but for the distance at which the form's bones simplify. */
    public static final String SIMPLIFY = MOD_ID + ":simplify_distance";

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        /* Before anything else: a mismatch here reads as "this addon does not fit this BBS build"
         * rather than as a crash on the first thing the user does. */
        BBSApi.requireVersion(MOD_ID, 1);

        /* Makes this addon's own assets addressable as bbslod:... links. */
        event.registerAddon(MOD_ID, BBSLod.class);
    }

    /**
     * Puts this addon's values on every form, including the ones BBS wrote and every body part.
     *
     * <p>The modifier runs on every constructed {@link mchorse.bbs_mod.forms.forms.Form} — root
     * forms, body parts, palette templates — so a body part can carry a lower cull distance than
     * its actor and drop out first.</p>
     */
    @Subscribe
    public void onFormModifiers(RegisterFormModifiersEvent event)
    {
        event.register((form) ->
        {
            form.add(new ValueFloat(CULL, -1F));
            form.add(new ValueFloat(SIMPLIFY, -1F));
        });
    }
}
