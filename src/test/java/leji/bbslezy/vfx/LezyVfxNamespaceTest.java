package leji.bbslezy.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Menjaga keputusan porting: tidak ada literal namespace asing (`bbsvfx`) yang
 * bocor ke kode add-on gabungan; paket VFX baru di bawah leji.bbslezy.vfx.
 */
class LezyVfxNamespaceTest
{
    @Test
    void vfxNamespacePackagesContainNoForeignModId() throws IOException
    {
        Path root = Path.of("src/main/java/leji/bbslezy/vfx");

        if (!Files.isDirectory(root))
        {
            /* D0 awal: direktori belum ada — hanya source pack yang diharapkan. */
            return;
        }

        try (Stream<Path> walk = Files.walk(root))
        {
            List<Path> offenders = walk
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p ->
                {
                    try
                    {
                        return Files.readString(p).contains("com.bbsvfx")
                            || Files.readString(p).contains("\"bbsvfx\"");
                    }
                    catch (IOException e)
                    {
                        return true;
                    }
                })
                .toList();

            assertTrue(offenders.isEmpty(), "bbsvfx leaked into: " + offenders);
        }
    }

    @Test
    void vfxMixinsJsonExists()
    {
        assertTrue(Files.exists(Path.of("src/main/resources/bbslezy.vfx.mixins.json")));
    }
}
