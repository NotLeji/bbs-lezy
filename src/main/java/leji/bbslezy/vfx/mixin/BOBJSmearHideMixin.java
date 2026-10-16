package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.SmearRenderState;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Per-bone smear isolation for BOBJ models.
 */
@Mixin(value = BOBJModelVAO.class, remap = false)
public abstract class BOBJSmearHideMixin
{
    @Shadow
    public BOBJLoader.CompiledData data;

    @Shadow
    public BOBJArmature armature;

    @Shadow
    private float[] tmpVertices;

    @Unique
    private String[] bbslezy$boneNames;

    @Inject(
        method = "updateMesh",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glBufferData(I[FI)V", ordinal = 0))
    private void bbslezy$collapseHiddenBones(StencilMap stencilMap, CallbackInfo ci)
    {
        Set<String> hide = SmearRenderState.hide;

        if (!SmearRenderState.active || hide == null || hide.isEmpty())
        {
            return;
        }

        if (!(SmearRenderState.target instanceof ModelFormRenderer renderer))
        {
            return;
        }

        ModelInstance model = renderer.getModel();

        if (model == null || !(model.model instanceof BOBJModel bobj) || !bobj.getVaos().contains((BOBJModelVAO) (Object) this))
        {
            return;
        }

        if (this.bbslezy$boneNames == null)
        {
            this.bbslezy$boneNames = new String[this.armature.orderedBones.size()];

            for (BOBJBone bone : this.armature.orderedBones)
            {
                this.bbslezy$boneNames[bone.index] = bone.name;
            }
        }

        float[] vertices = this.tmpVertices;
        int count = vertices.length / 3;
        boolean[] vertexHidden = new boolean[count];

        for (int i = 0; i < count; i++)
        {
            boolean hasWeight = false;
            boolean shown = false;

            for (int w = 0; w < 4; w++)
            {
                float weight = this.data.weightData[i * 4 + w];

                if (weight <= 0F)
                {
                    continue;
                }

                hasWeight = true;

                int boneIndex = this.data.boneIndexData[i * 4 + w];
                String name = boneIndex >= 0 && boneIndex < this.bbslezy$boneNames.length ? this.bbslezy$boneNames[boneIndex] : null;

                if (name == null || !hide.contains(name))
                {
                    shown = true;
                    break;
                }
            }

            vertexHidden[i] = hasWeight && !shown;
        }

        for (int t = 0; t * 3 + 2 < count; t++)
        {
            int v0 = t * 3;
            int v1 = t * 3 + 1;
            int v2 = t * 3 + 2;

            if (!vertexHidden[v0] && !vertexHidden[v1] && !vertexHidden[v2])
            {
                continue;
            }

            float cx = 0F;
            float cy = 0F;
            float cz = 0F;
            int shown = 0;

            for (int v = v0; v <= v2; v++)
            {
                if (!vertexHidden[v])
                {
                    cx += vertices[v * 3];
                    cy += vertices[v * 3 + 1];
                    cz += vertices[v * 3 + 2];
                    shown++;
                }
            }

            if (shown > 0)
            {
                cx /= shown;
                cy /= shown;
                cz /= shown;
            }
            else
            {
                cx = vertices[v0 * 3];
                cy = vertices[v0 * 3 + 1];
                cz = vertices[v0 * 3 + 2];
            }

            for (int v = v0; v <= v2; v++)
            {
                if (vertexHidden[v])
                {
                    vertices[v * 3] = cx;
                    vertices[v * 3 + 1] = cy;
                    vertices[v * 3 + 2] = cz;
                }
            }
        }
    }
}
