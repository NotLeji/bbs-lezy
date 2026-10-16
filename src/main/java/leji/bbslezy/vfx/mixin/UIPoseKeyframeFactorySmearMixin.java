package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.ISmearPoseEditor;
import leji.bbslezy.vfx.client.UILinesKeyframeFactory;
import leji.bbslezy.vfx.client.UISmearKeyframeFactory;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory.UIPoseFactoryEditor;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Re-adds smear or motion lines UI blocks in the pose keyframe factory layout.
 */
@Mixin(value = UIPoseKeyframeFactory.class, remap = false)
public abstract class UIPoseKeyframeFactorySmearMixin
{
    @Shadow
    public UIPoseFactoryEditor poseEditor;

    @Unique
    private static Field bbslezy$groupsField;

    @Unique
    private UIElement bbslezy$boneList()
    {
        try
        {
            if (bbslezy$groupsField == null)
            {
                bbslezy$groupsField = UIPoseEditor.class.getField("groups");
            }

            return (UIElement) bbslezy$groupsField.get(this.poseEditor);
        }
        catch (ReflectiveOperationException | ClassCastException e)
        {
            return null;
        }
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void bbslezy$readdSmear(CallbackInfo ci)
    {
        if (!(this.poseEditor instanceof ISmearPoseEditor smear))
        {
            return;
        }

        if ((Object) this instanceof UISmearKeyframeFactory)
        {
            this.poseEditor.removeAll();

            UIElement boneList = this.bbslezy$boneList();
            UIElement bones = boneList == null
                ? UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE))
                : UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE), boneList);

            if (((UIElement) (Object) this).getFlex().getW() > 240)
            {
                this.poseEditor.add(UI.row(smear.bbslezy$smearBlock(), bones));
            }
            else
            {
                this.poseEditor.add(bones, smear.bbslezy$smearBlock());
            }

            for (UIElement child : ((UIKeyframeFactory) (Object) this).scroll.getChildren(UIElement.class))
            {
                child.noCulling();
            }
        }
        else if ((Object) this instanceof UILinesKeyframeFactory)
        {
            this.poseEditor.add(smear.bbslezy$linesBlock());
        }
    }
}
