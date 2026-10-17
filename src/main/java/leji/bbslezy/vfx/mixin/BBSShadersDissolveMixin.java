package leji.bbslezy.vfx.mixin;

import leji.bbslezy.vfx.client.BbsVfxImpactShader;
import leji.bbslezy.vfx.client.SmearDissolveShader;
import mchorse.bbs_mod.client.BBSShaders;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes BBS model rendering through our model_dissolve program.
 */
@Mixin(value = BBSShaders.class, remap = false)
public abstract class BBSShadersDissolveMixin
{
    @Inject(method = "setup", at = @At("TAIL"))
    private static void bbslezy$buildDissolve(CallbackInfo ci)
    {
        SmearDissolveShader.build();
        BbsVfxImpactShader.build();
    }

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private static void bbslezy$useDissolve(CallbackInfoReturnable<ShaderProgram> cir)
    {
        ShaderProgram dissolve = SmearDissolveShader.get();

        if (dissolve != null)
        {
            cir.setReturnValue(dissolve);
        }
    }
}
