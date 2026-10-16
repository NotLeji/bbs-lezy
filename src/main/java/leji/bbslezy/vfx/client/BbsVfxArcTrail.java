package leji.bbslezy.vfx.client;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates the world positions a smeared bone passes through during the arc echo copies.
 */
public final class BbsVfxArcTrail
{
    public static boolean active;
    /** Per bone: the hand-tip world position at each copy (farthest → nearest). */
    public static final Map<String, List<Vector3f>> points = new LinkedHashMap<>();
    /** Per bone: the bone's own origin (the pivot the limb swings around) — apex of the swept fan. */
    public static final Map<String, Vector3f> pivots = new LinkedHashMap<>();

    private BbsVfxArcTrail()
    {}

    public static void begin()
    {
        active = true;
        points.clear();
        pivots.clear();
    }

    public static void add(String bone, Vector3f pos)
    {
        points.computeIfAbsent(bone, k -> new ArrayList<>()).add(pos);
    }

    public static void setPivot(String bone, Vector3f pivot)
    {
        pivots.put(bone, pivot);
    }

    public static void end()
    {
        active = false;
        points.clear();
        pivots.clear();
    }
}
