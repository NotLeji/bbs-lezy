package leji.bbslezy.ui;

import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.joml.Vector3d;

public class LezyPreviewSnap
{
    public static double[] snapshot;
    public static boolean active;
    private static double lastSnapX = Double.NaN;
    private static double lastSnapY = Double.NaN;
    private static double lastSnapZ = Double.NaN;
    private static float lastSnapYaw = Float.NaN;
    private static float lastSnapPitch = Float.NaN;

    public static boolean shouldSnap(boolean setting, boolean hasContext, boolean controlling)
    {
        return setting && hasContext && !controlling;
    }

    public static boolean shouldHidePlayer(Entity entity)
    {
        if (!active || entity == null)
        {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
        {
            return false;
        }

        return isPlayerEntity(entity, mc.player, mc.cameraEntity);
    }

    public static boolean isPlayerEntity(Entity entity, Entity player, Entity cameraEntity)
    {
        if (entity == null)
        {
            return false;
        }

        if (entity == player || (cameraEntity != null && entity == cameraEntity))
        {
            return true;
        }

        if (player != null && (entity == player.getVehicle() || entity.hasPassengerDeep(player)))
        {
            return true;
        }

        return false;
    }

    public static void onManualTeleport(UIFilmPanel panel)
    {
        if (panel == null)
        {
            return;
        }

        Camera camera = panel.getWorldCamera();
        if (camera == null || camera.position == null)
        {
            return;
        }

        snapshot = new double[] {
            camera.position.x,
            camera.position.y,
            camera.position.z,
            MathUtils.toDeg(camera.rotation.y) - 180F,
            MathUtils.toDeg(camera.rotation.x)
        };
        active = true;
    }

    public static void onManualTeleport()
    {
        active = true;
    }

    public static void onEnter(UIFilmPanel panel)
    {
        try
        {
            if (LodSettings.snapPlayerToPreview == null || !LodSettings.snapPlayerToPreview.get())
            {
                return;
            }

            if (panel == null || panel.getContext() == null)
            {
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null)
            {
                return;
            }

            ClientPlayerEntity player = mc.player;
            snapshot = new double[] {
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch()
            };
            active = true;

            teleportToCamera(panel);
        }
        catch (Throwable ignored)
        {}
    }

    public static void onUpdate(UIFilmPanel panel)
    {
        try
        {
            if (LodSettings.snapPlayerToPreview == null || !LodSettings.snapPlayerToPreview.get())
            {
                if (active)
                {
                    onLeave(panel);
                }
                return;
            }

            if (!active)
            {
                onEnter(panel);
                return;
            }

            if (panel == null || panel.getContext() == null)
            {
                return;
            }

            if (panel.getController() != null && panel.getController().isControlling())
            {
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null)
            {
                return;
            }

            teleportToCamera(panel);
        }
        catch (Throwable ignored)
        {}
    }

    public static void onLeave(UIFilmPanel panel)
    {
        try
        {
            if (panel != null && panel.playerToCamera)
            {
                return;
            }

            if (active && snapshot != null)
            {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world != null && mc.player != null)
                {
                    PlayerUtils.teleport(snapshot[0], snapshot[1], snapshot[2], (float) snapshot[3], (float) snapshot[4]);
                }
            }
        }
        catch (Throwable ignored)
        {}
        finally
        {
            active = false;
            snapshot = null;
            lastSnapX = Double.NaN;
            lastSnapY = Double.NaN;
            lastSnapZ = Double.NaN;
            lastSnapYaw = Float.NaN;
            lastSnapPitch = Float.NaN;
        }
    }

    private static void teleportToCamera(UIFilmPanel panel)
    {
        Camera camera = panel.getWorldCamera();
        if (camera == null)
        {
            return;
        }

        Vector3d cameraPos = camera.position;
        if (cameraPos == null)
        {
            return;
        }

        float targetYaw = MathUtils.toDeg(camera.rotation.y) - 180F;
        float targetPitch = MathUtils.toDeg(camera.rotation.x);

        /* Deadband: avoid sending network packets if camera hasn't moved */
        if (!Double.isNaN(lastSnapX))
        {
            double dx = cameraPos.x - lastSnapX;
            double dy = cameraPos.y - lastSnapY;
            double dz = cameraPos.z - lastSnapZ;
            float dyaw = Math.abs(targetYaw - lastSnapYaw);
            float dpitch = Math.abs(targetPitch - lastSnapPitch);

            if (dx * dx + dy * dy + dz * dz < 0.0025D && dyaw < 0.5F && dpitch < 0.5F)
            {
                return;
            }
        }

        lastSnapX = cameraPos.x;
        lastSnapY = cameraPos.y;
        lastSnapZ = cameraPos.z;
        lastSnapYaw = targetYaw;
        lastSnapPitch = targetPitch;

        PlayerUtils.teleport(
            cameraPos.x, cameraPos.y, cameraPos.z,
            targetYaw,
            targetPitch
        );
    }
}
