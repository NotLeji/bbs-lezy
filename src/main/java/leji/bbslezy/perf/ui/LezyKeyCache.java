package leji.bbslezy.perf.ui;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Cache hasil scan-disk per-key (manager instance), full dependency
 * injected: clock, worker executor, dan main-thread executor bisa
 * diganti di test. Thread-safety eksplisit: map state ConcurrentHashMap,
 * field hasil walk volatile. Entry basi (lewat TTL) atau ter-invalidate
 * selalu memicu walk ulang di worker; callback selalu dieksekusi di
 * eksekutor main.
 */
public class LezyKeyCache
{
    static final long TTL_MILLIS = 2000L;

    private final Supplier<Long> clock;
    private final Executor worker;
    private final Executor main;
    /** Thread-safe: main thread (requestKeys, invalidate) & worker thread (tulis hasil walk) berbagi map ini. */
    private final Map<Object, Entry> states = new ConcurrentHashMap<>();

    private static final class Entry
    {
        volatile Collection<String> keys;
        volatile long refreshAt;
        final AtomicBoolean inFlight = new AtomicBoolean(false);
    }

    public LezyKeyCache(Supplier<Long> clock, Executor worker, Executor main)
    {
        this.clock = clock;
        this.worker = worker;
        this.main = main;
    }

    public <T> void requestKeys(T key, Supplier<? extends Collection<String>> walker, Consumer<Collection<String>> callback)
    {
        Entry entry = this.stateFor(key);
        Collection<String> cached = entry.keys;
        boolean fresh = cached != null && this.clock.get() - entry.refreshAt < TTL_MILLIS;

        if (fresh)
        {
            callback.accept(cached);

            return;
        }

        if (!entry.inFlight.compareAndSet(false, true))
        {
            if (cached != null)
            {
                callback.accept(cached); /* serve stale sementara refresh masih berjalan */
            }

            return;
        }

        this.worker.execute(() ->
        {
            try
            {
                Collection<String> keys = walker.get(); /* walk disk berat, di worker */

                entry.keys = keys;
                entry.refreshAt = this.clock.get();

                this.main.execute(() -> callback.accept(keys));
            }
            finally
            {
                entry.inFlight.set(false);
            }
        });
    }

    public void invalidate(Object key)
    {
        Entry entry = this.states.get(key);

        if (entry != null)
        {
            entry.refreshAt = Long.MIN_VALUE / 2;
        }
    }

    private Entry stateFor(Object key)
    {
        Entry entry = this.states.get(key);

        if (entry == null)
        {
            entry = new Entry();
            this.states.put(key, entry);
        }

        return entry;
    }
}
