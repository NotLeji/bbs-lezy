package leji.bbslezy.ui;

import mchorse.bbs_mod.utils.iris.IrisUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LezyIrisHelper
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");

    private static volatile boolean reflectionInitialized;
    private static Method getPipelineManagerMethod;
    private static Method getPipelineNullableMethod;
    private static Field renderTargetsField;
    private static Method getDepthTextureMethod;
    private static Field cachedDepthBufferVersionField;
    private static Method getIrisConfigMethod;
    private static Method areShadersEnabledMethod;
    private static Method toggleShadersMethod;

    public static boolean areShadersEnabled()
    {
        if (!FabricLoader.getInstance().isModLoaded("iris"))
        {
            return false;
        }

        try
        {
            if (getIrisConfigMethod == null)
            {
                Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
                getIrisConfigMethod = irisClass.getMethod("getIrisConfig");
            }

            Object config = getIrisConfigMethod.invoke(null);
            if (config == null)
            {
                return false;
            }

            if (areShadersEnabledMethod == null)
            {
                areShadersEnabledMethod = config.getClass().getMethod("areShadersEnabled");
            }

            return (Boolean) areShadersEnabledMethod.invoke(config);
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }
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
            boolean enabled = areShadersEnabled();

            if (toggleShadersMethod == null)
            {
                Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
                toggleShadersMethod = irisClass.getMethod("toggleShaders", MinecraftClient.class, boolean.class);
            }

            toggleShadersMethod.invoke(null, MinecraftClient.getInstance(), !enabled);
        }
        catch (Throwable ignored)
        {
            /* Intentionally silent: a failed toggle must never break the editor */
        }
    }

    public static void syncPipelineDepthTarget()
    {
        if (!FabricLoader.getInstance().isModLoaded("iris"))
        {
            return;
        }

        try
        {
            Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();

            if (fb == null || !initReflection())
            {
                return;
            }

            Object pipelineManager = getPipelineManagerMethod.invoke(null);

            if (pipelineManager == null)
            {
                return;
            }

            Object pipeline = getPipelineNullableMethod.invoke(pipelineManager);

            if (pipeline == null || !renderTargetsField.getDeclaringClass().isInstance(pipeline))
            {
                return;
            }

            Object renderTargets = renderTargetsField.get(pipeline);

            if (renderTargets == null)
            {
                return;
            }

            int irisDepthTex = (Integer) getDepthTextureMethod.invoke(renderTargets);
            int activeDepthTex = fb.getDepthAttachment();

            if (activeDepthTex > 0 && irisDepthTex != activeDepthTex)
            {
                cachedDepthBufferVersionField.setInt(renderTargets, -1);
            }
        }
        catch (Throwable ignored)
        {}
    }

    private static boolean initReflection()
    {
        if (reflectionInitialized)
        {
            return cachedDepthBufferVersionField != null;
        }

        synchronized (LezyIrisHelper.class)
        {
            if (reflectionInitialized)
            {
                return cachedDepthBufferVersionField != null;
            }

            try
            {
                Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
                Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
                Class<?> irisPipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
                Class<?> renderTargetsClass = Class.forName("net.irisshaders.iris.targets.RenderTargets");

                getPipelineManagerMethod = irisClass.getMethod("getPipelineManager");
                getPipelineNullableMethod = pipelineManagerClass.getMethod("getPipelineNullable");

                Field rtField = irisPipelineClass.getDeclaredField("renderTargets");
                rtField.setAccessible(true);
                renderTargetsField = rtField;

                getDepthTextureMethod = renderTargetsClass.getMethod("getDepthTexture");

                Field versionField = renderTargetsClass.getDeclaredField("cachedDepthBufferVersion");
                versionField.setAccessible(true);
                cachedDepthBufferVersionField = versionField;
            }
            catch (Throwable t)
            {
                LOG.warn("failed to initialize Iris depth target reflection", t);
            }

            reflectionInitialized = true;
            return cachedDepthBufferVersionField != null;
        }
    }

    public static void onShaderpackLoaded()
    {
        try
        {
            MinecraftClient.getInstance().execute(() ->
            {
                try
                {
                    syncPipelineDepthTarget();

                    if (!IrisUtils.isRenderingOffscreen())
                    {
                        IrisUtils.setMainBound(true);
                    }
                }
                catch (Throwable t)
                {
                    LOG.warn("failed to reconcile iris state after shaderpack load", t);
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
        syncPipelineDepthTarget();
    }
}
