package leji.bbslezy.ui;

import leji.bbslezy.LodSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.joml.Vector3d;

public class LezyPreviewSnap
{
    public static double[] snapshot;
    public static boolean active;

    public static boolean shouldSnap(boolean setting, boolean hasContext, boolean controlling)
    {
        return setting && hasContext && !controlling;
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

        PlayerUtils.teleport(
            cameraPos.x, cameraPos.y, cameraPos.z,
            MathUtils.toDeg(camera.rotation.y) - 180F,
            MathUtils.toDeg(camera.rotation.x)
        );
    }
}
