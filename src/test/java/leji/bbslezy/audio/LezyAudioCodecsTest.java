package leji.bbslezy.audio;

import leji.bbslezy.video.LezyCopyVideoImporter;
import mchorse.bbs_mod.importers.ImporterContext;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LezyAudioCodecsTest
{
    private static ImporterContext context(String name)
    {
        return new ImporterContext(List.of(new File("/tmp/" + name)), null);
    }

    @Test
    void audioExtensionsNeverClaimAVideo()
    {
        String[] audio = LezyAudioCodecs.copyExtensions();

        for (String extension : LezyAudioCodecs.videoExtensions())
        {
            for (String claimed : audio)
            {
                assertFalse(claimed.equals(extension), "audio importer claims " + extension);
            }

            assertTrue(LezyAudioCodecs.isVideo(extension));
            assertFalse(LezyAudioCodecs.shouldHandle(extension));
        }
    }

    @Test
    void copyImportersSplitAudioFromVideo()
    {
        LezyCopyVideoImporter video = new LezyCopyVideoImporter();
        LezyCopyAudioImporter audio = new LezyCopyAudioImporter();

        for (String name : new String[] {"clip.mp4", "clip.MOV", "clip.mkv", "clip.webm", "clip.avi", "clip.m4v"})
        {
            assertTrue(video.canImport(context(name)), name);
            assertFalse(audio.canImport(context(name)), name);
        }

        assertTrue(audio.canImport(context("song.mp3")));
        assertFalse(video.canImport(context("song.mp3")));
    }
}
