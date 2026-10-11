package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import leji.bbslezy.vfx.forms.LezyWindForm;

/**
 * Form editor wrapper for {@link LezyWindForm}. After a scan, the wind's DIRECTION handle is the editor's
 * viewport gizmo.
 */
public class UIWindForm extends UIForm<LezyWindForm>
{
    private final Transform pointTransform = new Transform();
    private UIPropTransform pointGizmo;

    public UIWindForm()
    {
        super();

        this.defaultPanel = new UIWindFormPanel(this);

        this.registerPanel(this.defaultPanel, IKey.constant("Wind"), LezyVfxIcons.EXPLOSION);
        this.registerDefaultPanels();
    }

    private boolean editingPoint()
    {
        return this.form != null && this.form.scanned.get();
    }

    public UIPropTransform pointGizmo()
    {
        if (this.pointGizmo == null)
        {
            this.pointGizmo = new UIPropTransform().callbacks(null, this::bbslezy$writePointBack).barBackground();
            this.pointGizmo.setTransform(this.pointTransform);
        }

        return this.pointGizmo;
    }

    public void syncPointWidget()
    {
        if (this.form == null)
        {
            return;
        }

        this.pointTransform.translate.set(this.form.pointX.get(), this.form.pointY.get(), this.form.pointZ.get());
        this.pointGizmo().setTransform(this.pointTransform);
    }

    private void bbslezy$writePointBack()
    {
        if (this.form == null)
        {
            return;
        }

        this.form.pointX.set(this.pointTransform.translate.x);
        this.form.pointY.set(this.pointTransform.translate.y);
        this.form.pointZ.set(this.pointTransform.translate.z);
    }

    @Override
    public UIPropTransform getEditableTransform()
    {
        if (this.editingPoint())
        {
            return this.pointGizmo();
        }

        return super.getEditableTransform();
    }

    @Override
    public Matrix4f getOrigin(float transition)
    {
        if (this.editingPoint())
        {
            return new Matrix4f(super.getOriginMatrix(transition)).translate(this.form.point());
        }

        return super.getOrigin(transition);
    }

    @Override
    public Matrix4f getOriginMatrix(float transition)
    {
        if (this.editingPoint())
        {
            return new Matrix4f(super.getOriginMatrix(transition)).translate(this.form.point());
        }

        return super.getOriginMatrix(transition);
    }
}
