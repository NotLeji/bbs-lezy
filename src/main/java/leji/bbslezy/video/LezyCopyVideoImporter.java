package leji.bbslezy.video;

import leji.bbslezy.audio.LezyAudioCodecs;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.ImporterUtils;
import mchorse.bbs_mod.importers.types.IImporter;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;

import java.io.File;
import java.nio.file.Files;

/**
 * Copies a dropped video into the {@code video} assets folder byte-identical, so a clip stays a
 * clip: BBS plays it from the link, and its audio track is decoded on demand through ffmpeg when
 * a film clip or the video itself asks for it. Landed in the folder the file was dropped on when
 * it has one, the video folder otherwise.
 */
public class LezyCopyVideoImporter implements IImporter
{
    @Override
    public IKey getName()
    {
        return L10n.lang("bbslezy.importer.copy_video");
    }

    @Override
    public File getDefaultFolder()
    {
        return BBSMod.getAssetsPath("video");
    }

    @Override
    public boolean canImport(ImporterContext context)
    {
        return ImporterUtils.checkFileExtension(context.files, LezyAudioCodecs.videoExtensions());
    }

    @Override
    public void importFiles(ImporterContext context)
    {
        for (File file : context.files)
        {
            try
            {
                File destination = context.getDestination(this);
                File target = new File(destination, ImporterUtils.getName(destination, file.getName()));

                destination.mkdirs();

                Files.copy(file.toPath(), target.toPath());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
