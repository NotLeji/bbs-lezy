package leji.bbslezy.vfx.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import leji.bbslezy.vfx.client.SmearDissolveShader;
import mchorse.bbs_mod.client.BBSShaders;
import net.minecraft.client.gl.ShaderProgram;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    }

    @ModifyReturnValue(method = "getModel", at = @At("RETURN"))
    private static ShaderProgram bbslezy$useDissolve(ShaderProgram original)
    {
        ShaderProgram dissolve = SmearDissolveShader.get();

        return dissolve != null ? dissolve : original;
    }
}
