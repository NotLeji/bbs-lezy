package leji.bbslezy.perf.ui;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generic async loader dengan fail-open: pekerjaan berat di-run sekali di
 * executor worker, callback selalu di-executor main. Kontrak null: `work`
 * boleh menghasilkan null (BBS `BaseManager.load` me-return null untuk
 * file corrupt, dan `UIDataDashboardPanel.fill` tahan null — F7).
 * `work.get()` melempar di worker → callback menerima null (tanda read
 * gagal, tanpa double-read file); executor langsung melempar → fallback
 * `work.get()` sekali di main, dan bila itu pun melempar → callback(null).
 */
public class LezyAsyncLoader
{
    private final Executor worker;
    private final Executor main;

    public LezyAsyncLoader(Executor worker, Executor main)
    {
        this.worker = worker;
        this.main = main;
    }

    public <T> void load(Supplier<T> work, Consumer<T> callback)
    {
        try
        {
            this.worker.execute(() ->
            {
                T temp;

                try
                {
                    temp = work.get(); /* dipanggil tepat satu kali per request */
                }
                catch (Throwable t)
                {
                    t.printStackTrace();

                    temp = null; /* kontrak null: BBS memperlakukan load gagal sebagai null (F7) */
                }

                final T value = temp;

                this.main.execute(() -> callback.accept(value));
            });
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            this.main.execute(() ->
            {
                try
                {
                    callback.accept(work.get()); /* fallback sekali di main */
                }
                catch (Throwable error)
                {
                    error.printStackTrace();
                    callback.accept(null);
                }
            });
        }
    }
}
