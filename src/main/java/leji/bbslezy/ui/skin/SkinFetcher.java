package leji.bbslezy.ui.skin;

import mchorse.bbs_mod.BBSMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SkinFetcher
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10L))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final String BASE_URL = "https://minecraft-inside.ru";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    public static void checkSkinExists(String nickname, Consumer<Boolean> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            try
            {
                String url = "https://minecraft-inside.ru/skins/nick/" + nickname + ".html";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15L))
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                {
                    return false;
                }
                Document doc = Jsoup.parse(response.body());
                return !doc.select("a[href*=download]").isEmpty()
                    || !doc.select("img[src*=skin]").isEmpty()
                    || !doc.select("img[src*=skins]").isEmpty();
            }
            catch (Exception e)
            {
                LOG.error("SkinFetcher checkSkinExists error: " + e.getMessage());
                return false;
            }
        }, executor).thenAccept(callback);
    }

    public static void downloadSkin(String nickname, Consumer<File> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            try
            {
                String url = "https://minecraft-inside.ru/skins/nick/" + nickname + ".html?download";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(30L))
                    .GET()
                    .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200)
                {
                    return null;
                }
                byte[] data = response.body();
                if (data == null || data.length < 100)
                {
                    return null;
                }
                File skinsFolder = new File(BBSMod.getAssetsFolder(), "skins");
                skinsFolder.mkdirs();
                File output = new File(skinsFolder, nickname + ".png");
                Files.write(output.toPath(), data);
                return output;
            }
            catch (Exception e)
            {
                LOG.error("SkinFetcher downloadSkin error: " + e.getMessage());
                return null;
            }
        }, executor).thenAccept(callback);
    }

    public static void searchSkinsByKeyword(String keyword, Consumer<List<SkinResult>> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            ArrayList<SkinResult> results = new ArrayList<>();
            try
            {
                String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                String url = "https://minecraft-inside.ru/skins/?q=" + encoded;
                LOG.info("Searching skins: " + url);
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(20L))
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200)
                {
                    LOG.warn("Search failed: HTTP " + response.statusCode());
                    return results;
                }
                Document doc = Jsoup.parse(response.body());
                Elements cards = doc.select("div.box.box_grass.post.skin");
                for (Element card : cards)
                {
                    try
                    {
                        Element titleLink = card.selectFirst("h2.box__title a");
                        Element previewLink = card.selectFirst("a.skin__preview");
                        Element img = card.selectFirst("a.skin__preview img");
                        if (titleLink == null || previewLink == null || img == null)
                        {
                            continue;
                        }
                        String title = titleLink.text();
                        String pageUrl = titleLink.attr("href");
                        String id = previewLink.attr("data-id");
                        String previewUrl = img.attr("src");
                        String downloadUrl = previewLink.attr("data-image");
                        if (previewUrl.startsWith("/"))
                        {
                            previewUrl = BASE_URL + previewUrl;
                        }
                        if (downloadUrl.startsWith("/"))
                        {
                            downloadUrl = BASE_URL + downloadUrl;
                        }
                        results.add(new SkinResult(id, title, previewUrl, downloadUrl, pageUrl));
                    }
                    catch (Exception e)
                    {
                        LOG.warn("Error parsing card: " + e.getMessage());
                    }
                }
                LOG.info("Parsed results: " + results.size());
            }
            catch (Exception e)
            {
                LOG.error("Search error: " + e.getMessage());
            }
            return results;
        }, executor).thenAccept(callback);
    }

    public static void downloadPreview(String imageUrl, String id, Consumer<File> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            try
            {
                File tempFolder = new File(BBSMod.getAssetsFolder(), "skins/.temp");
                tempFolder.mkdirs();
                File output = new File(tempFolder, id + ".png");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(20L))
                    .GET()
                    .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200)
                {
                    return null;
                }
                byte[] data = response.body();
                if (data == null || data.length < 100)
                {
                    return null;
                }
                Files.write(output.toPath(), data);
                return output;
            }
            catch (Exception e)
            {
                LOG.error("Preview download error: " + e.getMessage());
                return null;
            }
        }, executor).thenAccept(callback);
    }

    public static void downloadSkinByUrl(String imageUrl, String filename, Consumer<File> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            try
            {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(30L))
                    .GET()
                    .build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200)
                {
                    return null;
                }
                byte[] data = response.body();
                if (data == null || data.length < 100)
                {
                    return null;
                }
                File skinsFolder = new File(BBSMod.getAssetsFolder(), "skins");
                skinsFolder.mkdirs();
                File output = new File(skinsFolder, filename + ".png");
                Files.write(output.toPath(), data);
                return output;
            }
            catch (Exception e)
            {
                LOG.error("Skin download error: " + e.getMessage());
                return null;
            }
        }, executor).thenAccept(callback);
    }

    public static boolean isValidSkin(File png)
    {
        return true;
    }

    public static void cleanupAllPreviews()
    {
        try
        {
            File tempFolder = new File(BBSMod.getAssetsFolder(), "skins/.temp");
            if (!tempFolder.exists())
            {
                return;
            }
            File[] files = tempFolder.listFiles();
            if (files == null)
            {
                return;
            }
            int count = 0;
            for (File f : files)
            {
                if (!f.isFile() || !f.delete())
                {
                    continue;
                }
                count++;
            }
            LOG.info("cleanupAllPreviews: deleted " + count + " files from skins/.temp");
        }
        catch (Exception e)
        {
            LOG.error("cleanupAllPreviews error: " + e.getMessage());
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
        return img.getRGB(x, y) >> 24 & 0xFF;
    }
}
