package leji.bbslezy.perf.ui;

/**
 * Predicate pure untuk skip render world di balik dashboard opak:
 * world di-skip HANYA saat dashboard menutup window dengan warna solid
 * (background image null + tint alpha >= 1F) — bila tint tidak opak,
 * background image dipakai, atau panel/dashboard tidak cocok, world
 * wajib tetap dirender (F4 + Review Focus #4).
 */
public final class LezyUIWorldSkip
{
    public static boolean shouldSkip(
        boolean screenOpen,
        boolean dashboardActive,
        boolean panelNeedsBackground,
        float tintAlpha
    )
    {
        return screenOpen && dashboardActive && panelNeedsBackground && tintAlpha >= 1F;
    }

    private LezyUIWorldSkip()
    {}
}
