package leji.bbslezy.ui.skin;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SkinSearchTest
{
    @Test
    void testIsAlexNullOrMissing()
    {
        assertFalse(SkinFetcher.isAlex(null));
        assertFalse(SkinFetcher.isAlex(new File("nonexistent_skin.png")));
    }
}
