package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

/**
 * Exposes the per-bone smear / lines widgets built onto {@code UIPoseEditor} as two separate blocks.
 */
public interface ISmearPoseEditor
{
    UIElement bbslezy$smearBlock();

    UIElement bbslezy$linesBlock();
}
