package leji.bbslezy.perf.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LezyAsyncLoaderTest
{
    private static final Executor DIRECT = Runnable::run;

    @Test
    public void successCallsCallbackOnMainExecutor()
    {
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

        LezyAsyncLoader loader = new LezyAsyncLoader(worker, main);

        loader.load(() -> "payload", (value) -> marks.add("cb:" + value));

        assertEquals(List.of("worker", "main", "cb:payload"), marks);
    }

    @Test
    public void workerFailureDeliversNullOnMain()
    {
        List<String> marks = new ArrayList<>();
        Executor worker = (task) ->
        {
            marks.add("worker");
            task.run(); /* task.run() melempar di dalam load → catch internal loader */
        };
        Executor main = DIRECT;

        LezyAsyncLoader loader = new LezyAsyncLoader(worker, main);

        loader.load(() ->
        {
            throw new RuntimeException("simulated worker failure");
        }, (Consumer<String>) (value) -> marks.add("cb:" + value));

        assertEquals(List.of("worker", "cb:null"), marks, "worker gagal → callback menerima null di main, tanpa double-read file");
    }
}
