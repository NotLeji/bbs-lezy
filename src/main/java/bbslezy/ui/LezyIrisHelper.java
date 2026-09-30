package bbslezy.ui;

import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.utils.iris.IrisUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public class LezyIrisHelper
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");

    private static volatile boolean pendingShaderReload;

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

    public static void onShaderpackLoaded()
    {
        try
        {
            if (!IrisUtils.isShaderPackEnabled())
            {
                return;
            }

            if (UIScreen.getCurrentMenu() == null)
            {
                return;
            }

            pendingShaderReload = true;
            LOG.info("shaderpack loaded while a BBS editor UI is open; clean reload deferred until it closes");

            MinecraftClient.getInstance().execute(() ->
            {
                try
                {
                    if (!IrisUtils.isRenderingOffscreen())
                    {
                        IrisUtils.setMainBound(true);
                    }
                }
                catch (Throwable t)
                {
                    LOG.warn("failed to reconcile iris main-bound state", t);
                }
            });
        }
        catch (Throwable t)
        {
            LOG.warn("onShaderpackLoaded failed", t);
        }
    }

    public static void tick()
    {
        if (!pendingShaderReload || UIScreen.getCurrentMenu() != null)
        {
            return;
        }

        pendingShaderReload = false;

        if (!FabricLoader.getInstance().isModLoaded("iris"))
        {
            return;
        }

        try
        {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");

            irisClass.getMethod("reload").invoke(null);
            LOG.info("deferred clean shader reload after BBS editor closed");
        }
        catch (Throwable t)
        {
            LOG.warn("deferred shader reload failed", t);
        }
    }
}
