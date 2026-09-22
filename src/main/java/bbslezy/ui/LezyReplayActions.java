package bbslezy.ui;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.film.replays.ReplayListEntry;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The replay panel features that BBS's UI has no hook for: instant scroll to either end of the
 * list, and the three selections and duplicate modes a mass-produced scene needs.
 *
 * <p>None of the methods here hold state: they read the list, do the work against the film, and
 * hand the list back its own new selection. The list's own {@link UIReplayList#refreshReplayList()}
 * is what rebuilds the rows, so every caller ends in it — the selection is what that method
 * carries over the rebuild, so setting it before the refresh is what keeps the new pick.</p>
 *
 * <p>Selections are set through {@code selection.setAll}, not by mutating the list the caller
 * passed in: {@link UIReplayList#getSelectedReplays()} returns a fresh list every call, and
 * editing that copy moves nothing on screen.</p>
 */
public class LezyReplayActions
{
    /**
     * Every row in the list, the folders with the replays in them: the rows BBS draws are the
     * entries themselves, and a category the duplicates came in is one of them, so picking
     * everything means the category rows come along for whatever the user does to the selection
     * next — removing them, moving them, not only the replays inside.
     */
    public static void selectAll(UIReplayList list)
    {
        list.selection.setAll(list.getList());
        list.refreshReplayList();
    }

    /**
     * Everything that shares a model with the picked replays: the point is a duplicate farm,
     * where the copies are the ones the user wants as a set. Non-model forms fall back to the
     * whole form data, so two replays of the same billboard count as the same thing.
     */
    public static void selectSameModel(UIReplayList list, Film film)
    {
        Set<String> wanted = new HashSet<>();

        for (Replay replay : list.getSelectedReplays())
        {
            String identity = identityOf(replay.form.get());

            if (identity != null)
            {
                wanted.add(identity);
            }
        }

        if (wanted.isEmpty())
        {
            return;
        }

        List<ReplayListEntry> entries = new ArrayList<>();

        for (ReplayListEntry entry : list.getList())
        {
            if (entry.isReplay() && wanted.contains(identityOf(entry.replay.form.get())))
            {
                entries.add(entry);
            }
        }

        list.selection.setAll(entries);
        list.refreshReplayList();
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
     * @param category the folder the copies land in, or null to inherit each source's own
     * @return the last copy made, for the caller to scroll to
     */
    public static Replay duplicateToTotal(Film film, List<Replay> selected, int total, String category)
    {
        if (selected.isEmpty() || total <= 0)
        {
            return null;
        }

        Replay last = null;

        for (int round = 0; round < total; round++)
        {
            for (Replay source : selected)
            {
                last = copyReplay(film, source, category);
            }
        }

        return last;
    }

    private static Replay copyReplay(Film film, Replay source, String category)
    {
        Replay copy = film.replays.addReplay();

        copy.copy(source);

        if (category != null)
        {
            copy.category.set(category);
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
}
