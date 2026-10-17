package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import leji.bbslezy.vfx.client.IBlendPoseEditor;
import leji.bbslezy.vfx.client.LezyVfxUI;
import leji.bbslezy.vfx.forms.BlendMode;
import leji.bbslezy.vfx.forms.IBlendBone;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adds the {@code blend} channel's controls to the pose editor: a WHOLE-MODEL section (toggle + mode +
 * strength, stored in the {@link IBlendBone#WHOLE_BONE} sentinel of the pose) and a per-bone "Bone blend"
 * block (mode + strength for the selected bone(s)). Both edit the same keyframed pose, so the whole-vs-per
 * choice and all parameters live in this one track and apply immediately. Mirrors {@code UIPoseEditorSmearMixin}.
 */
@Mixin(value = UIPoseEditor.class, remap = false)
public abstract class UIPoseEditorBlendMixin implements IBlendPoseEditor
{
    @Shadow
    private Pose pose;

    @Shadow
    private void forEachSelectedPose(Consumer<? super PoseTransform> consumer)
    {
        throw new AssertionError();
    }

    @Unique private UIToggle bbslezy$wholeToggle;
    @Unique private UIButton bbslezy$wholeMode;
    @Unique private UITrackpad bbslezy$wholeFactor;
    @Unique private UIButton bbslezy$blendMode;
    @Unique private UITrackpad bbslezy$blendFactor;
    @Unique private UIElement bbslezy$blendBlock;

    @Override
    public UIElement bbslezy$blendBlock()
    {
        return this.bbslezy$blendBlock;
    }

    @Override
    public void bbslezy$syncBlend()
    {
        this.bbslezy$syncWhole();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addBlend(CallbackInfo ci)
    {
        /* Whole-model: applies one blend to the entire form (stored in the WHOLE_BONE sentinel). */
        this.bbslezy$wholeToggle = new UIToggle(IKey.constant("Blend whole model"), (b) ->
        {
            IBlendBone w = this.bbslezy$whole();

            if (w != null)
            {
                if (b.getValue())
                {
                    if (w.bbslezy$blendMode() == BlendMode.NORMAL.ordinal())
                    {
                        w.bbslezy$setBlendMode(BlendMode.ADD.ordinal());
                    }
                }
                else
                {
                    w.bbslezy$setBlendMode(BlendMode.NORMAL.ordinal());
                }

                this.bbslezy$syncWhole();
            }
        });

        this.bbslezy$wholeMode = new UIButton(IKey.constant("Blend mode"), (b) -> this.bbslezy$openWholePicker());

        this.bbslezy$wholeFactor = new UITrackpad((v) ->
        {
            IBlendBone w = this.bbslezy$whole();

            if (w != null)
            {
                w.bbslezy$setBlendFactor(v.floatValue());
            }
        });
        this.bbslezy$wholeFactor.limit(0, 1).values(0.05F);
        this.bbslezy$wholeFactor.tooltip(IKey.constant("Whole-model blend strength"));

        /* Per-bone: applies to the selected bone(s). */
        this.bbslezy$blendMode = new UIButton(IKey.constant("Blend mode"), (b) -> this.bbslezy$openPicker());

        this.bbslezy$blendFactor = new UITrackpad((v) -> this.bbslezy$forEach((s) -> s.bbslezy$setBlendFactor(v.floatValue())));
        this.bbslezy$blendFactor.limit(0, 1).values(0.05F);
        this.bbslezy$blendFactor.tooltip(IKey.constant("Blend strength"));

        this.bbslezy$blendBlock = UI.column(
            LezyVfxUI.section(IKey.constant("Whole model"), this.bbslezy$wholeToggle, this.bbslezy$wholeMode, this.bbslezy$wholeFactor),
            LezyVfxUI.section(IKey.constant("Bone blend"), this.bbslezy$blendMode, this.bbslezy$blendFactor));
    }

    /** The whole-model sentinel entry (auto-created on the pose), or null if no pose. */
    @Unique
    private IBlendBone bbslezy$whole()
    {
        return this.pose == null ? null : (IBlendBone) this.pose.getOrCreate(IBlendBone.WHOLE_BONE);
    }

    @Unique
    private void bbslezy$forEach(Consumer<IBlendBone> action)
    {
        this.forEachSelectedPose((pt) -> action.accept((IBlendBone) pt));
    }

    @Unique
    private void bbslezy$openWholePicker()
    {
        UIListOverlayPanel overlay = new UIListOverlayPanel(IKey.constant("Blend mode"), (value) ->
        {
            int index = BlendMode.DISPLAY_NAMES.indexOf(value);
            IBlendBone w = this.bbslezy$whole();

            if (index >= 0 && w != null)
            {
                w.bbslezy$setBlendMode(index);
                this.bbslezy$syncWhole();
            }
        });

        overlay.addValues(BlendMode.DISPLAY_NAMES);
        UIOverlay.addOverlay(((UIElement) (Object) this).getContext(), overlay);
    }

    @Unique
    private void bbslezy$openPicker()
    {
        UIListOverlayPanel overlay = new UIListOverlayPanel(IKey.constant("Blend mode"), (value) ->
        {
            int index = BlendMode.DISPLAY_NAMES.indexOf(value);

            if (index >= 0)
            {
                this.bbslezy$forEach((s) -> s.bbslezy$setBlendMode(index));
                this.bbslezy$blendMode.label = IKey.constant(value);
            }
        });

        overlay.addValues(BlendMode.DISPLAY_NAMES);
        UIOverlay.addOverlay(((UIElement) (Object) this).getContext(), overlay);
    }

    @Unique
    private void bbslezy$syncWhole()
    {
        PoseTransform wp = this.pose == null ? null : this.pose.transforms.get(IBlendBone.WHOLE_BONE);
        int wm = wp instanceof IBlendBone w ? w.bbslezy$blendMode() : 0;

        this.bbslezy$wholeToggle.setValue(wm != BlendMode.NORMAL.ordinal());
        this.bbslezy$wholeMode.label = IKey.constant(BlendMode.byIndex(wm).display);
        this.bbslezy$wholeFactor.setValue(wp instanceof IBlendBone w ? w.bbslezy$blendFactor() : 1F);
    }

    @Inject(method = "pickBones", at = @At("TAIL"))
    private void bbslezy$sync(List<String> bones, CallbackInfo ci)
    {
        this.bbslezy$syncWhole();

        if (bones.isEmpty() || this.pose == null)
        {
            this.bbslezy$blendMode.label = IKey.constant(BlendMode.NORMAL.display);
            this.bbslezy$blendFactor.setValue(1F);

            return;
        }

        IBlendBone s = (IBlendBone) this.pose.getOrCreate(bones.get(0));

        this.bbslezy$blendMode.label = IKey.constant(BlendMode.byIndex(s.bbslezy$blendMode()).display);
        this.bbslezy$blendFactor.setValue(s.bbslezy$blendFactor());
    }
}
