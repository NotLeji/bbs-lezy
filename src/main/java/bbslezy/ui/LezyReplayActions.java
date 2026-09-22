package bbslezy.ui;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.Replays;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.ui.film.replays.ReplayListEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The replay panel features that BBS's UI has no hook for: instant scroll to either end of the
 * list, and the three selections and duplicate modes a mass-produced scene needs.
 *
 * <p>None of the methods here hold state: they take the list, do the work against the film, and
 * hand the list back its own selection to redraw. The list's own rebuild is what makes the new
 * rows appear, so every caller ends in {@code refreshReplayList}.</p>
 *
 * <p>Reaching into the UI classes is outside the {@code api} contract: these methods are the
 * whole reason the addon mixes into BBS, and they would break in the game rather than at the
 * build if the classes moved. Every reference is kept to public fields and methods, so a rename
 * is what would break it, and the mixin's target list is what surfaces the break.</p>
 */
public class LezyReplayActions
{
    /* ---------------------------- selection ---------------------------- */

    /**
     * Every replay in the film, whether its folder is open or not — the list's own select-all
     * walks only visible rows, and a collapsed category full of duplicates is exactly the case
     * where that misses the ones the user means.
     */
    public static void selectAll(Replays replays, List<Replay> selected, Runnable refresh)
    {
        selected.clear();
        selected.addAll(replays.getList());

        refresh.run();
    }

    /**
     * Everything that shares a model with the picked replays: the point is a duplicate farm,
     * where the copies are the ones the user wants as a set. Non-model forms fall back to the
     * whole form data, so two replays of the same billboard count as the same thing.
     */
    public static void selectSameModel(Film film, List<Replay> selected, Runnable refresh)
    {
        if (selected.isEmpty())
        {
            return;
        }

        Set<String> wanted = new HashSet<>();

        for (Replay replay : selected)
        {
            String identity = identityOf(replay.form.get());

            if (identity != null)
            {
                wanted.add(identity);
            }
        }

        List<Replay> same = new ArrayList<>();

        for (Replay replay : film.replays.getList())
        {
            if (wanted.contains(identityOf(replay.form.get())))
            {
                same.add(replay);
            }
        }

        selected.clear();
        selected.addAll(same);

        refresh.run();
    }

    private static String identityOf(Form form)
    {
        if (form instanceof ModelForm model)
        {
            String modelId = model.model.get();

            return modelId.isEmpty() ? null : modelId;
        }

        return form == null ? null : form.toData().toString();
    }

    /* ---------------------------- duplicate ---------------------------- */

    /**
     * The number asked for is a target for the whole selection, not a per-replay count: asking for
     * 150 with three replays picked makes 150 copies, not 450, because what the user is counting
     * is the crowd, not the actors in it.
     *
     * <p>One copy of each picked replay is added per round, so the distribution across the
     * selection stays even — the first copy of each precedes the second of any, and a request
     * that does not divide evenly leaves the remainder spread over the first picks rather than
     * piled on one. An empty selection adds nothing.</p>
     *
     * @param duplicates the category the copies land in, or null to inherit each source's own
     */
    public static Replay duplicateToTotal(Film film, List<Replay> selected, int total, String duplicates, Runnable refresh)
    {
        if (selected.isEmpty() || total <= 0)
        {
            return null;
        }

        List<Replay> copies = new ArrayList<>();

        for (int round = 0; round < total; round++)
        {
            for (Replay source : selected)
            {
                copies.add(copyReplay(film, source, duplicates));
            }
        }

        refresh.run();

        return copies.isEmpty() ? null : copies.get(copies.size() - 1);
    }

    private static Replay copyReplay(Film film, Replay source, String duplicates)
    {
        Replay copy = film.replays.addReplay();

        copy.copy(source);

        if (duplicates != null)
        {
            copy.category.set(duplicates);
        }

        return copy;
    }

    /**
     * A name for the category the copies of one duplicate operation share, so a second run lands
     * in its own rather than merging with the first: the count makes it unique within the film
     * without the user having to type it.
     */
    public static String nextDuplicateCategory(Film film, String prefix)
    {
        Set<String> used = new HashSet<>();

        for (Replay replay : film.replays.getList())
        {
            used.add(replay.category.get());
        }

        for (int i = 1; i < 10000; i++)
        {
            String candidate = prefix + " " + i;

            if (!used.contains(candidate))
            {
                return candidate;
            }
        }

        return prefix;
    }

    /* ---------------------------- scroll ---------------------------- */

    public static int scrollEnd(List<ReplayListEntry> list, int itemSize)
    {
        int replays = 0;

        for (ReplayListEntry entry : list)
        {
            if (entry.isReplay())
            {
                replays++;
            }
        }

        return Math.max(0, replays - 1) * itemSize;
    }
}
