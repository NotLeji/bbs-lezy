package leji.bbslezy.perf.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LezyKeyCacheTest
{
    private static class ManualClock implements Supplier<Long>
    {
        long value;

        @Override
        public Long get()
        {
            return this.value;
        }
    }

    private static final Executor DIRECT = Runnable::run;

    @Test
    public void freshEntryAvoidsWalk()
    {
        ManualClock clock = new ManualClock();
        AtomicInteger walks = new AtomicInteger();
        List<Collection<String>> delivered = new ArrayList<>();
        Supplier<List<String>> walker = () ->
        {
            walks.incrementAndGet();

            return List.of("a", "b");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);

        cache.requestKeys("films", walker, delivered::add);
        cache.requestKeys("films", walker, delivered::add);

        assertEquals(1, walks.get(), "cache fresh harus mencegah walk kedua");
        assertEquals(2, delivered.size());
        assertTrue(delivered.get(1).contains("a"));
    }

    @Test
    public void staleEntryReWalksAfterTtl()
    {
        ManualClock clock = new ManualClock();
        AtomicInteger walks = new AtomicInteger();
        Supplier<List<String>> walker = () ->
        {
            walks.incrementAndGet();

            return List.of("a");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);

        cache.requestKeys("films", walker, (k) -> {});

        clock.value = LezyKeyCache.TTL_MILLIS + 1L;   /* melewati TTL */

        cache.requestKeys("films", walker, (k) -> {});

        assertEquals(2, walks.get(), "entry basi harus memicu walk ulang");
    }

    @Test
    public void invalidateAfterMutation()
    {
        ManualClock clock = new ManualClock();
        AtomicInteger walks = new AtomicInteger();
        Supplier<List<String>> walker = () ->
        {
            walks.incrementAndGet();

            return List.of("a");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);

        cache.requestKeys("films", walker, (k) -> {});
        cache.invalidate("films");                    /* simulasi save/delete BBS */
        cache.requestKeys("films", walker, (k) -> {});

        assertEquals(2, walks.get(), "invalidasi harus memicu walk ulang sebelum TTL lewat");
    }

    @Test
    public void callbackRunsOnMainExecutorOnly()
    {
        ManualClock clock = new ManualClock();
        List<String> marks = new ArrayList<>();
        Executor worker = (task) ->
        {
            marks.add("worker");
            task.run();
        };
        Executor main = (task) ->
        {
            marks.add("main");
            task.run();
        };
        Supplier<List<String>> walker = List::of;

        LezyKeyCache cache = new LezyKeyCache(clock, worker, main);

        cache.requestKeys("films", walker, (k) -> {});

        assertEquals(List.of("worker", "main"), marks, "walk di worker, callback di eksekutor main");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void inFlightDedupesConcurrentRequests()
    {
        ManualClock clock = new ManualClock();
        AtomicInteger walks = new AtomicInteger();
        List<String> order = new ArrayList<>();
        final LezyKeyCache[] holder = new LezyKeyCache[1];
        final Supplier<List<String>>[] walkerHolder = new Supplier[1];

        walkerHolder[0] = () ->
        {
            walks.incrementAndGet();
            order.add("walk");
            holder[0].requestKeys("films", walkerHolder[0], (k) -> order.add("second-callback"));

            return List.of("a");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);

        holder[0] = cache;

        cache.requestKeys("films", walkerHolder[0], (k) -> order.add("first-callback"));

        assertEquals(1, walks.get(), "request selama in-flight tidak boleh memicu walk kedua");
        assertEquals(List.of("walk", "first-callback", "second-callback"), order);
    }

    @Test
    public void inFlightCacheMissDeliversToAllWaiters()
    {
        ManualClock clock = new ManualClock();
        List<String> firstResult = new ArrayList<>();
        List<String> secondResult = new ArrayList<>();
        final LezyKeyCache[] holder = new LezyKeyCache[1];
        final Supplier<List<String>>[] walkerHolder = new Supplier[1];

        walkerHolder[0] = () ->
        {
            holder[0].requestKeys("films", walkerHolder[0], secondResult::addAll);
            return List.of("x", "y");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);
        holder[0] = cache;

        cache.requestKeys("films", walkerHolder[0], firstResult::addAll);

        assertEquals(List.of("x", "y"), firstResult);
        assertEquals(List.of("x", "y"), secondResult, "caller yang request saat in-flight (cache miss) tidak boleh di-drop");
    }

    @Test
    public void walkerExceptionDoesNotStrandCallback()
    {
        ManualClock clock = new ManualClock();
        List<Collection<String>> results = new ArrayList<>();
        Supplier<List<String>> brokenWalker = () ->
        {
            throw new RuntimeException("disk error");
        };

        LezyKeyCache cache = new LezyKeyCache(clock, DIRECT, DIRECT);
        cache.requestKeys("broken", brokenWalker, results::add);

        assertEquals(1, results.size(), "callback harus tetap dipanggil meski walker throw exception");
        assertTrue(results.get(0).isEmpty(), "fallback empty list saat error");
    }
}
