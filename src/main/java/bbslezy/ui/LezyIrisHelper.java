package bbslezy.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

public class LezyIrisHelper
{
    /**
     * Toggle shaders on/off via reflection to avoid compile dependency on Iris.
     * Replicates Iris toggle keybind logic.
     */
    public static void toggleShaders()
    {
        if (!FabricLoader.getInstance().isModLoaded("iris"))
        {
            return;
        }

        try
        {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Method getConfigMethod = irisClass.getMethod("getIrisConfig");
            Object config = getConfigMethod.invoke(null);
            Method areEnabledMethod = config.getClass().getMethod("areShadersEnabled");
            boolean enabled = (Boolean) areEnabledMethod.invoke(config);

            Method toggleMethod = irisClass.getMethod("toggleShaders", MinecraftClient.class, boolean.class);
            toggleMethod.invoke(null, MinecraftClient.getInstance(), !enabled);
        }
        catch (Throwable ignored)
        {
            /* Intentionally silent: a failed toggle must never break the editor */
        }
    }
}
