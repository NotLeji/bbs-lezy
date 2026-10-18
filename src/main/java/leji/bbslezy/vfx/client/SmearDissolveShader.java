package leji.bbslezy.vfx.client;

import leji.bbslezy.BBSLod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;

import java.io.IOException;

/**
 * Model shader copy with Dissolve uniform for smear frames.
 */
public final class SmearDissolveShader
{
    private static ShaderProgram program;

    private SmearDissolveShader()
    {}

    public static ShaderProgram get()
    {
        return program;
    }

    public static void build()
    {
        if (program != null)
        {
            program.close();
            program = null;
        }

        try
        {
            program = new ShaderProgram(bbslezy$factory(), "model_dissolve", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static ResourceFactory bbslezy$factory()
    {
        return (id) ->
        {
            ResourceFactory manager = MinecraftClient.getInstance().getResourceManager();

            if (id.getPath().contains("/core/"))
            {
                return manager.getResource(Identifier.of(BBSLod.MOD_ID, id.getPath()));
            }

            return manager.getResource(id);
        };
    }
}
