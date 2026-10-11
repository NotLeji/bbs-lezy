package leji.bbslezy.vfx.resources;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Source pack addon VFX di dalam bbslezy — override asset bbs-namespace mis. tex
 * atlas icon VFX (registerFirst memastikan pack ini membaca lebih awal di AssetProvider).
 *
 * Keputusan port: prefix internal pakai assets/bbslezy/override_vfx (unik & tidak bentrok
 * dengan assets/bbslezy/assets yang sudah dipakai pack bbslezy utama).
 */
public class LezyVfxSourcePack implements ISourcePack
{
    private static final Class<?> ANCHOR = LezyVfxSourcePack.class;
    private static final String INTERNAL = "assets/bbslezy/override_vfx";

    /** Path asset (di bawah {@link Link#ASSETS}) yang di-overwrite; hidup per fase sesuai kebutuhan. */
    private static final Set<String> OVERRIDES = Set.of(
        /* fase D6: textures/bbsvfx_icons.png hadir sementara — dihapus saat port */
    );

    @Override
    public String getPrefix()
    {
        return Link.ASSETS;
    }

    @Override
    public boolean hasAsset(Link link)
    {
        return Link.ASSETS.equals(link.source)
            && OVERRIDES.contains(link.path)
            && ANCHOR.getResource("/" + INTERNAL + "/" + link.path) != null;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        InputStream stream = ANCHOR.getResourceAsStream("/" + INTERNAL + "/" + link.path);

        if (stream == null)
        {
            throw new FileNotFoundException(link.toString());
        }

        return stream;
    }

    @Override
    public File getFile(Link link)
    {
        return null;
    }

    @Override
    public Link getLink(File file)
    {
        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
    }
}
