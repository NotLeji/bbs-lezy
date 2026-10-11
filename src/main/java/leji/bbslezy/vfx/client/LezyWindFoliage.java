package leji.bbslezy.vfx.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import leji.bbslezy.vfx.forms.DestructionBlock;
import leji.bbslezy.vfx.forms.DestructionBlockList;
import leji.bbslezy.vfx.forms.LezyWindForm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The NON-DESTRUCTIVE foliage sway for a directly-placed {@link LezyWindForm}. It removes the
 * zone's foliage from the CLIENT render only ({@code ClientWorld.setBlockState}) and draws swaying proxies
 * ({@link ExplosionFoliageVAO#renderWind}) in its place.
 */
public final class LezyWindFoliage
{
    private static final DestructionBlockList PROXY = new DestructionBlockList("wind_proxy");
    private static final List<BlockPos> CUT_POS = new ArrayList<>();
    private static final List<BlockState> CUT_OLD = new ArrayList<>();

    private static boolean scanned;
    private static int scanCx, scanCy, scanCz;
    private static double sx, sy, sz;
    private static int sRadius;
    private static int unseen;

    private LezyWindFoliage()
    {}
    private static int healCooldown;

    public static void tick()
    {
        if (isActive() && ++unseen > 40)
        {
            restore();
        }
    }

    public static boolean isActive()
    {
        return !CUT_POS.isEmpty();
    }

    public static int scanCx()
    {
        return scanCx;
    }

    public static int scanCy()
    {
        return scanCy;
    }

    public static int scanCz()
    {
        return scanCz;
    }

    public static List<DestructionBlock> proxyBlocks()
    {
        return PROXY.getAllTyped();
    }

    public static void ensure(LezyWindForm form, double ox, double oy, double oz, int radius)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        if (world == null)
        {
            return;
        }

        unseen = 0;

        if (scanned && (radius != sRadius
            || Math.abs(ox - sx) > 1.0 || Math.abs(oy - sy) > 1.0 || Math.abs(oz - sz) > 1.0))
        {
            replaceClient(world, CUT_POS, CUT_OLD);
            reset();
        }

        int cx = (int) Math.round(ox), cy = (int) Math.round(oy), cz = (int) Math.round(oz);

        if (!scanned)
        {
            scanCx = cx;
            scanCy = cy;
            scanCz = cz;
            sx = ox;
            sy = oy;
            sz = oz;
            sRadius = radius;

            int rSq = radius * radius;

            List<BlockPos> cand = new ArrayList<>();
            List<BlockState> candState = new ArrayList<>();
            Set<Long> logs = new HashSet<>();
            Set<Long> leaves = new HashSet<>();

            for (int x = cx - radius; x <= cx + radius; x++)
            {
                for (int z = cz - radius; z <= cz + radius; z++)
                {
                    int ddx = x - cx, ddz = z - cz;

                    if (ddx * ddx + ddz * ddz > rSq || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4))
                    {
                        continue;
                    }

                    int top = Math.max(world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z), cy + 48);

                    for (int y = Math.max(world.getBottomY(), cy - 8); y < top; y++)
                    {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(pos);

                        if (state.isAir() || !DestructionCapture.isFoliage(state))
                        {
                            continue;
                        }

                        cand.add(pos);
                        candState.add(state);

                        if (state.isIn(BlockTags.LOGS))
                        {
                            logs.add(pos.asLong());
                        }
                        else if (state.isIn(BlockTags.LEAVES))
                        {
                            leaves.add(pos.asLong());
                        }
                    }
                }
            }

            Set<Long> treeLogs = treeLogs(logs, leaves);

            for (int i = 0; i < cand.size(); i++)
            {
                BlockPos pos = cand.get(i);
                BlockState state = candState.get(i);

                if (state.isIn(BlockTags.LOGS) && !treeLogs.contains(pos.asLong()))
                {
                    continue;
                }

                int tint = mc.getBlockColors().getColor(state, world, pos, 0);
                int light = DestructionCapture.captureLight(world, pos);

                PROXY.addBlock(pos.getX() - cx, pos.getY() - cy, pos.getZ() - cz, state, tint, light);
                CUT_POS.add(pos);
                CUT_OLD.add(state);
                world.setBlockState(pos, fillFor(state), 2);
            }

            scanned = true;

            return;
        }

        /* Self-heal throttled to once every 20 frames to avoid querying/setting hundreds of blocks per frame */
        if (++healCooldown >= 20)
        {
            healCooldown = 0;
            for (int i = 0; i < CUT_POS.size(); i++)
            {
                BlockPos pos = CUT_POS.get(i);
                BlockState fill = fillFor(CUT_OLD.get(i));

                if (world.getBlockState(pos) != fill)
                {
                    world.setBlockState(pos, fill, 2);
                }
            }
        }
    }

    private static BlockState fillFor(BlockState cut)
    {
        return DestructionCapture.fillFor(cut);
    }

    private static Set<Long> treeLogs(Set<Long> logs, Set<Long> leaves)
    {
        Set<Long> tree = new HashSet<>();

        if (leaves.isEmpty() || logs.isEmpty())
        {
            return tree;
        }

        Set<Long> seen = new HashSet<>();
        List<Long> body = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();

        for (Long start : logs)
        {
            if (!seen.add(start))
            {
                continue;
            }

            body.clear();
            queue.clear();
            queue.add(start);
            body.add(start);

            boolean leafy = false;

            while (!queue.isEmpty())
            {
                long cur = queue.poll();
                int bx = BlockPos.unpackLongX(cur), by = BlockPos.unpackLongY(cur), bz = BlockPos.unpackLongZ(cur);

                for (int dx = -1; dx <= 1; dx++)
                {
                    for (int dy = -1; dy <= 1; dy++)
                    {
                        for (int dz = -1; dz <= 1; dz++)
                        {
                            if (dx == 0 && dy == 0 && dz == 0)
                            {
                                continue;
                            }

                            long nb = BlockPos.asLong(bx + dx, by + dy, bz + dz);

                            if (leaves.contains(nb))
                            {
                                leafy = true;
                            }
                            else if (logs.contains(nb) && seen.add(nb))
                            {
                                queue.add(nb);
                                body.add(nb);
                            }
                        }
                    }
                }
            }

            if (leafy)
            {
                tree.addAll(body);
            }
        }

        return tree;
    }

    public static void restore()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world != null)
        {
            replaceClient(mc.world, CUT_POS, CUT_OLD);
        }

        reset();
    }

    private static void replaceClient(ClientWorld world, List<BlockPos> pos, List<BlockState> old)
    {
        for (int i = 0; i < pos.size(); i++)
        {
            if (world.getBlockState(pos.get(i)) == fillFor(old.get(i)))
            {
                world.setBlockState(pos.get(i), old.get(i), 2);
            }
        }
    }

    private static void reset()
    {
        PROXY.clearBlocks();
        CUT_POS.clear();
        CUT_OLD.clear();
        scanned = false;
    }
}
