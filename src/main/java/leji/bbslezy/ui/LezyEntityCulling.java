package leji.bbslezy.ui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.lang.reflect.Method;

/**
 * Reads EntityCulling's per-entity occlusion state via reflection (no compile dep).
 *
 * <p>Why this exists: EntityCulling skips occluded entities in {@code WorldRenderer} before
 * {@code ActorEntityRenderer} runs, while LOD ranks controller StubEntity copies. Without the
 * bridge, an occluded actor still eats LOD quota, and LOD-visible ranking can fight the
 * occlusion result. Both directions are read-only: LOD never writes EntityCulling flags.</p>
 */
public class LezyEntityCulling
{
    private static volatile boolean resolved;
    private static Method isCulledMethod;
    private static Method ignoresCullingMethod;

    public static boolean isLoaded()
    {
        try
        {
            return FabricLoader.getInstance().isModLoaded("entityculling");
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /**
     * Whether EntityCulling currently considers this entity occluded (skipped in world render).
     * False when the mod is absent, reflection failed, or the entity opts out.
     */
    public static boolean isOccluded(Entity entity)
    {
        if (entity == null || !isLoaded() || !resolve())
        {
            return false;
        }

        try
        {
            if (ignoresCullingMethod != null && Boolean.TRUE.equals(ignoresCullingMethod.invoke(null, entity)))
            {
                return false;
            }

            return Boolean.TRUE.equals(isCulledMethod.invoke(entity));
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    /**
     * Whether EntityCulling currently considers the entity with this id occluded.
     * Looks up the live client entity; false when the world is gone.
     */
    public static boolean isOccludedById(int entityId)
    {
        try
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc == null || mc.world == null)
            {
                return false;
            }

            return isOccluded(mc.world.getEntityById(entityId));
        }
        catch (Throwable ignored)
        {
            return false;
        }
    }

    private static boolean resolve()
    {
        if (resolved)
        {
            return isCulledMethod != null;
        }

        resolved = true;

        try
        {
            /* Cullable is mixed into Entity: dev.tr7zw.entityculling.access.Cullable#isCulled(). */
            Class<?> cullable = Class.forName("dev.tr7zw.entityculling.access.Cullable");
            isCulledMethod = cullable.getMethod("isCulled");

            try
            {
                Class<?> helper = Class.forName("dev.tr7zw.entityculling.NMSCullingHelper");
                ignoresCullingMethod = helper.getMethod("ignoresCulling", Entity.class);
            }
            catch (Throwable ignored)
            {
                ignoresCullingMethod = null;
            }
        }
        catch (Throwable ignored)
        {
            isCulledMethod = null;
            ignoresCullingMethod = null;
        }

        return isCulledMethod != null;
    }
}
