package leji.bbslezy.ui.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import mchorse.bbs_mod.utils.resources.PlayerSkinImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SkinFetcher
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10L))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    public static void fetchMojangSkin(String nickname, Consumer<File> callback)
    {
        /* Check online players on server first (must run on main thread) */
        String onlineUrl = getOnlinePlayerSkinUrl(nickname);

        CompletableFuture.supplyAsync(() ->
        {
            try
            {
                String skinUrl = onlineUrl != null ? onlineUrl : getSkinUrlFromMojang(nickname);
                if (skinUrl == null)
                {
                    return null;
                }

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(skinUrl))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15L))
                    .GET()
                    .build();
                HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200 || response.body() == null || response.body().length < 100)
                {
                    return null;
                }

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
                if (image == null)
                {
                    return null;
                }

                BufferedImage normalized = PlayerSkinImage.normalize(image);

                /* Write strictly to OS temporary folder for preview */
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "bbslezy_skins");
                tempDir.mkdirs();
                File tempFile = new File(tempDir, nickname.toLowerCase() + ".png");
                ImageIO.write(normalized, "png", tempFile);
                tempFile.deleteOnExit();

                return tempFile;
            }
            catch (Exception e)
            {
                LOG.error("Failed to fetch Mojang skin for " + nickname, e);
                return null;
            }
        }, EXECUTOR).thenAccept(callback);
    }
    public static void cleanupTempFolder()
    {
        try
        {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "bbslezy_skins");
            if (tempDir.exists() && tempDir.isDirectory())
            {
                File[] files = tempDir.listFiles();
                if (files != null)
                {
                    for (File f : files)
                    {
                        f.delete();
                    }
                }
            }
        }
        catch (Throwable ignored)
        {}
    }


    private static String getOnlinePlayerSkinUrl(String nickname)
    {
        try
        {
            ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            if (handler == null)
            {
                return null;
            }

            for (PlayerListEntry entry : handler.getPlayerList())
            {
                GameProfile profile = entry.getProfile();
                if (nickname.equalsIgnoreCase(profile.getName()))
                {
                    for (Property property : profile.getProperties().get("textures"))
                    {
                        String url = skinUrlFromTextures(property.getValue());
                        if (url != null)
                        {
                            return url;
                        }
                    }
                }
            }
        }
        catch (Exception ignored)
        {}

        return null;
    }

    private static String getSkinUrlFromMojang(String nickname) throws Exception
    {
        HttpRequest profileRequest = HttpRequest.newBuilder()
            .uri(URI.create(PROFILE_URL + nickname))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(10L))
            .GET()
            .build();
        HttpResponse<String> profileResponse = CLIENT.send(profileRequest, HttpResponse.BodyHandlers.ofString());
        if (profileResponse.statusCode() != 200 || profileResponse.body() == null || profileResponse.body().isEmpty())
        {
            return null;
        }

        JsonObject profileObj = JsonParser.parseString(profileResponse.body()).getAsJsonObject();
        JsonElement idElement = profileObj.get("id");
        if (idElement == null)
        {
            return null;
        }

        String uuid = idElement.getAsString();
        HttpRequest sessionRequest = HttpRequest.newBuilder()
            .uri(URI.create(SESSION_URL + uuid))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(10L))
            .GET()
            .build();
        HttpResponse<String> sessionResponse = CLIENT.send(sessionRequest, HttpResponse.BodyHandlers.ofString());
        if (sessionResponse.statusCode() != 200 || sessionResponse.body() == null || sessionResponse.body().isEmpty())
        {
            return null;
        }

        JsonObject sessionObj = JsonParser.parseString(sessionResponse.body()).getAsJsonObject();
        JsonArray properties = sessionObj.getAsJsonArray("properties");
        if (properties == null)
        {
            return null;
        }

        for (JsonElement prop : properties)
        {
            JsonObject propObj = prop.getAsJsonObject();
            if ("textures".equals(propObj.get("name").getAsString()))
            {
                String url = skinUrlFromTextures(propObj.get("value").getAsString());
                if (url != null)
                {
                    return url;
                }
            }
        }

        return null;
    }

    private static String skinUrlFromTextures(String base64)
    {
        try
        {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            JsonObject textures = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures");
            if (textures == null || !textures.has("SKIN"))
            {
                return null;
            }
            return textures.getAsJsonObject("SKIN").get("url").getAsString();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static boolean isAlex(File png)
    {
        if (png == null || !png.exists())
        {
            return false;
        }
        try
        {
            BufferedImage img = ImageIO.read(png);
            if (img == null)
            {
                return false;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            if (h < 64)
            {
                return false;
            }
            int scale = w / 64;
            int a1 = alpha(img, 54 * scale, 20 * scale);
            int a2 = alpha(img, 46 * scale, 52 * scale);
            return a1 == 0 && a2 == 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static int alpha(BufferedImage img, int x, int y)
    {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight())
        {
            return -1;
        }
        return (img.getRGB(x, y) >> 24) & 0xFF;
    }
}
