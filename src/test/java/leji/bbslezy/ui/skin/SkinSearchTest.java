package leji.bbslezy.ui.skin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SkinSearchTest
{
    @Test
    void testSkinResult()
    {
        SkinResult result = new SkinResult("123", "Cool Skin", "https://example.com/p.png", "https://example.com/d.png", "https://example.com/page");
        assertEquals("123", result.id);
        assertEquals("Cool Skin", result.title);
        assertEquals("https://example.com/p.png", result.previewUrl);
        assertEquals("https://example.com/d.png", result.downloadUrl);
        assertEquals("https://example.com/page", result.pageUrl);
    }

    @Test
    void testSanitizeFilename()
    {
        assertEquals("skin", SkinSearchByKeywordsPanel.sanitize(null));
        assertEquals("skin", SkinSearchByKeywordsPanel.sanitize(""));
        assertEquals("Cool_Skin_123", SkinSearchByKeywordsPanel.sanitize("Cool Skin 123"));
        assertEquals("Skin_Test", SkinSearchByKeywordsPanel.sanitize("Skin: Test?"));
        assertEquals("a".repeat(40), SkinSearchByKeywordsPanel.sanitize("a".repeat(60)));
    }
}
