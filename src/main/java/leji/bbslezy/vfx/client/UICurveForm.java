package leji.bbslezy.vfx.client;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.joml.Matrix4f;
import leji.bbslezy.vfx.forms.CurveForm;
import leji.bbslezy.vfx.forms.CurvePoint;

/**
 * Form editor wrapper for {@link CurveForm}.
 */
public class UICurveForm extends UIForm<CurveForm>
{
    private UIPropTransform pointGizmo;
    private CurvePoint selectedPoint;

    public UICurveForm()
    {
        super();

        this.defaultPanel = new UICurveFormPanel(this);

        this.registerPanel(this.defaultPanel, IKey.constant("Curve"), Icons.CURVES);
        this.registerDefaultPanels();
    }

    public UIPropTransform pointGizmo()
    {
        if (this.pointGizmo == null)
        {
            this.pointGizmo = new UIPropTransform().callbacks(() -> this.form.pose).barBackground();
        }

        return this.pointGizmo;
    }

    public void selectPoint(CurvePoint point)
    {
        this.selectedPoint = point;
        this.pointGizmo().setTransform(point == null ? null : this.form.pointTransformOf(point));
        this.updateGizmoMode();
    }

    private boolean editingPoint()
    {
        return this.form != null && this.selectedPoint != null && this.view == this.defaultPanel;
    }

    private void updateGizmoMode()
    {
    }

    @Override
    public void setPanel(UIFormPanel<CurveForm> panel)
    {
        super.setPanel(panel);
        this.updateGizmoMode();
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
            return new Matrix4f(super.getOriginMatrix(transition)).translate(this.form.pointPosition(this.selectedPoint));
        }

        return super.getOrigin(transition);
    }

    @Override
    public Matrix4f getOriginMatrix(float transition)
    {
        if (this.editingPoint())
        {
            return new Matrix4f(super.getOriginMatrix(transition)).translate(this.form.pointPosition(this.selectedPoint));
        }

        return super.getOriginMatrix(transition);
    }
}
