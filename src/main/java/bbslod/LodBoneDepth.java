package bbslod;

import mchorse.bbs_mod.cubic.data.model.ModelGroup;

import java.util.WeakHashMap;

/**
 * How deep a bone sits in its rig, cached per {@link ModelGroup}.
 *
 * <p>Identity-matched ({@link ModelGroup} does not override {@code equals}) and weak-keyed, so
 * entries die with the cached model they describe — models are shared per asset key in
 * {@code BBSModClient.getModels()}, so this cache is bounded by the scene's unique rigs.</p>
 */
public class LodBoneDepth
{
    private static final WeakHashMap<ModelGroup, Integer> CACHE = new WeakHashMap<>();

    public static int depth(ModelGroup group)
    {
        Integer cached = CACHE.get(group);

        if (cached != null)
        {
            return cached;
        }

        int depth = 0;

        for (ModelGroup parent = group.parent; parent != null; parent = parent.parent)
        {
            depth++;
        }

        CACHE.put(group, depth);

        return depth;
    }
}
