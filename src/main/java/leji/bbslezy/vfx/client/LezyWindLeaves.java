package leji.bbslezy.vfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import leji.bbslezy.BBSLod;
import leji.bbslezy.vfx.forms.DestructionBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flying leaves for the WindForm — small leaf-tinted sprites shed from the zone's real crowns, tumbling
 * downwind and fading, layered over the streaks/dust. Rendered in the FORM's local frame.
 */
public final class LezyWindLeaves
{
    private static final int MAX_LEAVES = 60;

    private static final Identifier WHITE_TEX = new Identifier(BBSLod.MOD_ID, "wind_leaf_white");
    private static boolean texReady;

    private static final List<float[]> CROWNS = new ArrayList<>();
    private static double cx, cy, cz;
    private static int cRadius = -1;
    private static boolean scanned;
    private static int scanCx, scanCy, scanCz;

    private LezyWindLeaves()
    {}

    public static void render(MatrixStack stack, double ox, double oy, double oz,
        float windX, float windZ, double tFlow, float density, float opacity, int radius)
    {
        render(stack, ox, oy, oz, windX, windZ, tFlow, density, opacity, radius, false, 0F, 0F, 0F, 0F, 1F);
    }

    public static void render(MatrixStack stack, double ox, double oy, double oz,
        float windX, float windZ, double tFlow, float density, float opacity, int radius,
        boolean vortex, float coreR, float swirl, float suction, float updraft, float windScale)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;

        if (world == null || density <= 0.001F || opacity <= 0.001F)
        {
            return;
        }

        ensureCrowns(world, mc, ox, oy, oz, radius);

        if (CROWNS.isEmpty())
        {
            return;
        }

        ensureTexture();

        stack.push();
        stack.translate(scanCx - ox, scanCy - oy, scanCz - oz);

        Matrix4f mat = stack.peek().getPositionMatrix();

        Camera camera = mc.gameRenderer.getCamera();
        Quaternionf camRot = camera.getRotation();
        Vector3f camRight = camRot.transform(new Vector3f(1F, 0F, 0F));
        Vector3f camUp = camRot.transform(new Vector3f(0F, 1F, 0F));

        int crownCount = CROWNS.size();
        int slots = Math.round(MAX_LEAVES * density);
        float alphaScale = opacity;

        BufferBuilder buffer = LezyVfxRenderCompat.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        boolean any = false;

        for (int i = 0; i < slots; i++)
        {
            float[] crown = CROWNS.get((int) ((i * 2654435761L >>> 16) % crownCount));
            float h1 = hash(i, 11), h2 = hash(i, 12), h3 = hash(i, 13), h4 = hash(i, 14),
                h5 = hash(i, 15), h6 = hash(i, 16);

            float period = 2.6F + 3.4F * h1;
            double phased = tFlow + h2 * period;
            float tau = (float) (phased - Math.floor(phased / period) * period);
            float life = period * 0.9F;

            if (tau >= life)
            {
                continue;
            }

            float drift = (1.2F + 1.6F * h3) * windScale;
            float fall = 0.7F + 0.7F * h4;
            float wob = (float) Math.sin(tau * (2.5F + 2F * h5) + h6 * 6.2832F);

            float sx = (h3 - 0.5F) * 2.2F, sz = (h4 - 0.5F) * 2.2F;

            float px, py, pz;

            if (vortex)
            {
                float x0 = crown[0] + sx, z0 = crown[2] + sz;
                float r0 = (float) Math.sqrt(x0 * x0 + z0 * z0);
                float core = Math.max(coreR, 0.5F);
                float omega = swirl / Math.max(r0, core) * 0.35F;
                float th = (float) Math.atan2(z0, x0) + omega * tau;

                float pull = suction * (core / Math.max(r0, core)) * 0.5F;
                float rr = Math.max(core * 0.5F, r0 - pull * tau);

                px = rr * (float) Math.cos(th) + wob * 0.4F;
                pz = rr * (float) Math.sin(th) + wob * 0.4F;
                py = crown[1] + 0.6F + updraft * (core / Math.max(r0, core)) * 0.35F * tau + 0.15F * wob;
            }
            else
            {
                px = crown[0] + sx + windX * drift * tau + wob * 0.5F;
                py = crown[1] + 0.6F - fall * tau + 0.15F * wob;
                pz = crown[2] + sz + windZ * drift * tau + wob * 0.5F;
            }

            float fade = (float) Math.sin(Math.PI * tau / life);
            float alpha = fade * alphaScale;

            if (alpha < 0.02F)
            {
                continue;
            }

            float spin = (h5 - 0.5F) * 6F * tau + h6 * 6.2832F;
            float cs = (float) Math.cos(spin), sn = (float) Math.sin(spin);
            float len = 0.15F + 0.09F * h6;
            float wid = len * 0.5F;

            float rx = (camRight.x * cs + camUp.x * sn) * wid, ry = (camRight.y * cs + camUp.y * sn) * wid, rz = (camRight.z * cs + camUp.z * sn) * wid;
            float ux = (-camRight.x * sn + camUp.x * cs) * len, uy = (-camRight.y * sn + camUp.y * cs) * len, uz = (-camRight.z * sn + camUp.z * cs) * len;

            float shade = 0.72F + 0.4F * h6;
            float r = Math.min(1F, crown[3] * shade), g = Math.min(1F, crown[4] * shade), b = Math.min(1F, crown[5] * shade);
            int packedLight = (int) crown[6];

            leaf(buffer, mat, px, py, pz, rx, ry, rz, ux, uy, uz, r, g, b, alpha, packedLight);
            any = true;
        }

        if (!any)
        {
            buffer.end();
            stack.pop();

            return;
        }

        RenderSystem.setShaderTexture(0, WHITE_TEX);
        RenderSystem.setShader(GameRenderer::getParticleProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        mc.gameRenderer.getLightmapTextureManager().enable();

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        mc.gameRenderer.getLightmapTextureManager().disable();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);

        stack.pop();
    }

    private static void leaf(BufferBuilder b, Matrix4f m, float x, float y, float z,
        float rx, float ry, float rz, float ux, float uy, float uz,
        float r, float g, float bl, float a, int light)
    {
        LezyVfxRenderCompat.next(b.vertex(m, x + ux, y + uy, z + uz).texture(0.5F, 0F).color(r, g, bl, a).light(light));
        LezyVfxRenderCompat.next(b.vertex(m, x + rx, y + ry, z + rz).texture(1F, 0.5F).color(r, g, bl, a).light(light));
        LezyVfxRenderCompat.next(b.vertex(m, x - ux * 0.55F, y - uy * 0.55F, z - uz * 0.55F).texture(0.5F, 1F).color(r, g, bl, a).light(light));
        LezyVfxRenderCompat.next(b.vertex(m, x - rx, y - ry, z - rz).texture(0F, 0.5F).color(r, g, bl, a).light(light));
    }

    private static void ensureTexture()
    {
        if (texReady)
        {
            return;
        }

        NativeImage img = new NativeImage(2, 2, false);

        for (int y = 0; y < 2; y++)
        {
            for (int x = 0; x < 2; x++)
            {
                img.setColor(x, y, 0xFFFFFFFF);
            }
        }

        MinecraftClient.getInstance().getTextureManager().registerTexture(WHITE_TEX, new NativeImageBackedTexture(img));
        texReady = true;
    }

    private static void ensureCrowns(ClientWorld world, MinecraftClient mc, double ox, double oy, double oz, int radius)
    {
        boolean stable = scanned && radius == cRadius
            && Math.abs(ox - cx) < 1.0 && Math.abs(oy - cy) < 1.0 && Math.abs(oz - cz) < 1.0;

        if (stable && !CROWNS.isEmpty())
        {
            return;
        }

        CROWNS.clear();

        int oxi = (int) Math.round(ox), oyi = (int) Math.round(oy), ozi = (int) Math.round(oz);

        if (LezyWindFoliage.isActive())
        {
            crownsFromProxies();

            scanCx = LezyWindFoliage.scanCx();
            scanCy = LezyWindFoliage.scanCy();
            scanCz = LezyWindFoliage.scanCz();
        }

        if (CROWNS.isEmpty())
        {
            crownsFromWorld(world, mc, oxi, oyi, ozi, radius);

            scanCx = oxi;
            scanCy = oyi;
            scanCz = ozi;
        }

        /* Fallback: if there are no tree leaves around (e.g. flat test world, void, indoor, or newly placed),
         * generate virtual seed emitters in a circle so leaves are never invisible. */
        if (CROWNS.isEmpty())
        {
            int fallbackRadius = Math.min(radius, 24);
            int light = WorldRenderer.getLightmapCoordinates(world, new BlockPos(oxi, oyi, ozi));
            for (int angle = 0; angle < 360; angle += 45)
            {
                double rad = Math.toRadians(angle);
                float rx = (float) (Math.cos(rad) * fallbackRadius * 0.5);
                float rz = (float) (Math.sin(rad) * fallbackRadius * 0.5);
                CROWNS.add(new float[] {rx, 2.0F, rz, 0.32F, 0.55F, 0.22F, light});
            }
            scanCx = oxi;
            scanCy = oyi;
            scanCz = ozi;
        }

        cx = ox;
        cy = oy;
        cz = oz;
        cRadius = radius;
        scanned = true;
    }

    private static void crownsFromProxies()
    {
        Map<Long, float[]> tops = new HashMap<>();

        for (DestructionBlock b : LezyWindFoliage.proxyBlocks())
        {
            if (!b.blockState().isIn(BlockTags.LEAVES))
            {
                continue;
            }

            int lx = b.x.get(), ly = b.y.get(), lz = b.z.get();
            long key = (((long) lx) & 0xFFFFF) | ((((long) lz) & 0xFFFFF) << 20);
            float[] cur = tops.get(key);

            if (cur == null || ly > cur[1])
            {
                int tint = b.tint.get();
                float r = ((tint >> 16) & 0xFF) / 255F, g = ((tint >> 8) & 0xFF) / 255F, bl = (tint & 0xFF) / 255F;

                if (tint == -1)
                {
                    r = 0.32F; g = 0.52F; bl = 0.20F;
                }

                tops.put(key, new float[] {lx, ly, lz, r, g, bl, b.light.get()});
            }
        }

        CROWNS.addAll(tops.values());
    }

    private static void crownsFromWorld(ClientWorld world, MinecraftClient mc, int oxi, int oyi, int ozi, int radius)
    {
        int rSq = radius * radius;

        for (int x = oxi - radius; x <= oxi + radius; x += 2)
        {
            for (int z = ozi - radius; z <= ozi + radius; z += 2)
            {
                int ddx = x - oxi, ddz = z - ozi;

                if (ddx * ddx + ddz * ddz > rSq || !world.getChunkManager().isChunkLoaded(x >> 4, z >> 4))
                {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
                BlockPos pos = new BlockPos(x, top, z);
                BlockState state = world.getBlockState(pos);

                if (!state.isIn(BlockTags.LEAVES))
                {
                    continue;
                }

                int tint = mc.getBlockColors().getColor(state, world, pos, 0);
                int light = WorldRenderer.getLightmapCoordinates(world, pos);

                CROWNS.add(new float[] {
                    x - oxi, top - oyi, z - ozi,
                    ((tint >> 16) & 0xFF) / 255F, ((tint >> 8) & 0xFF) / 255F, (tint & 0xFF) / 255F, light});
            }
        }
    }

    public static void reset()
    {
        scanned = false;
        cRadius = -1;
        CROWNS.clear();
    }

    private static float hash(int i, int salt)
    {
        int x = (i ^ 0x9e3779b9) * 0x85ebca6b + salt * 0x165667b1;

        x ^= x >>> 13;
        x *= 0x27d4eb2d;
        x ^= x >>> 15;

        return (x & 0xFFFF) / (float) 0xFFFF;
    }
}
