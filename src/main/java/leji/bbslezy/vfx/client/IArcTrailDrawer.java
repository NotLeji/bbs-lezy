package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

/**
 * Implemented by {@code ModelFormRenderer} (via {@code ModelFormRendererLinesMixin}) so the smear render
 * can trigger the arc motion-trail draw after the echo copies.
 */
public interface IArcTrailDrawer
{
    void bbslezy$drawArcTrail(FormRenderingContext context);
}
