package leji.bbslezy.perf.ui;

import mchorse.bbs_mod.utils.manager.FolderManager;
import net.minecraft.client.MinecraftClient;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Static facade LezyKeyCache untuk dipakai mixin: satu worker thread
 * "BBS Lezy UI I/O Worker", callback ke main thread Minecraft. Mutator
 * BBS (save/rename/delete/dll) di-hook via FolderManagerMutationsMixin
 * untuk memanggil invalidate().
 */
public final class LezyDataKeysCache
{
    static final ExecutorService WORKER = Executors.newSingleThreadExecutor((runnable) ->
    {
        Thread thread = new Thread(runnable, "BBS Lezy UI I/O Worker");

        thread.setDaemon(true); /* supaya JVM bisa keluar setelah bench mc.scheduleStop() */

        return thread;
    });
    /* jam dalam millis — selaras dengan TTL_MILLIS (LezyKeyCache) */
    private static final LezyKeyCache CACHE = new LezyKeyCache(
        System::currentTimeMillis, WORKER, LezyDataKeysCache::minecraftExecute
    );

    /**
     * Main-thread executor yang dievaluasi lambat: jangan bind
     * `MinecraftClient.getInstance()` saat class load karena bisa masih
     * null; method statis ini memanggilnya tepat saat dieksekusi.
     */
    static void minecraftExecute(Runnable task)
    {
        MinecraftClient.getInstance().execute(task);
    }

    public static void requestKeys(FolderManager<?> manager, Consumer<Collection<String>> callback)
    {
        try
        {
            CACHE.requestKeys(manager, manager::getKeys, callback);
        }
        catch (Throwable t)
        {
            t.printStackTrace(); /* fail-open: walk sync asli tetap jalan di caller */

            callback.accept(manager.getKeys());
        }
    }

    public static void invalidate(FolderManager<?> manager)
    {
        CACHE.invalidate(manager);
    }

    private LezyDataKeysCache()
    {}
}
