package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import leji.bbslezy.vfx.forms.BlendMode;

/**
 * Keyframe editor for the {@code blend_mode} channel: a dropdown of {@link BlendMode} names (like the
 * label editor's blend picker) instead of the numeric trackpad the integer factory would give. Picking a
 * mode writes the selected keyframe's value via {@link #setValue}. Bound by registering this against the
 * dedicated {@code LezyVfxKeyframeFactories.BLEND_MODE} factory instance.
 */
public class UIBlendModeKeyframeFactory extends UIKeyframeFactory<Integer>
{
    private final UIButton button;

    public UIBlendModeKeyframeFactory(Keyframe<Integer> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.button = new UIButton(IKey.constant(bbslezy$label(keyframe.getValue())), (b) -> this.bbslezy$openPicker());
        this.scroll.add(this.button);
    }

    private static String bbslezy$label(Integer index)
    {
        return BlendMode.byIndex(index == null ? 0 : index).display;
    }

    private void bbslezy$openPicker()
    {
        UIListOverlayPanel overlay = new UIListOverlayPanel(IKey.constant("Blend mode"), (value) ->
        {
            int index = BlendMode.DISPLAY_NAMES.indexOf(value);

            if (index >= 0)
            {
                this.setValue(index);
                this.button.label = IKey.constant(value);
            }
        });

        overlay.addValues(BlendMode.DISPLAY_NAMES);
        overlay.setValue(bbslezy$label(this.keyframe.getValue()));

        UIOverlay.addOverlay(this.getContext(), overlay);
    }
}
