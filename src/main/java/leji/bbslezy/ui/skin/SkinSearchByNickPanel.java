package leji.bbslezy.ui.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSResources;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.resources.PlayerSkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SkinSearchByNickPanel extends UIOverlayPanel
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");

    private final UIFormEditor editor;

    private String nickname = "";
    private UILabel resultLabel;
    private UIButton findButton;
    private UIButton applyButton;
    private Texture previewTexture;
    private UIRenderable previewRenderer;
    private SkinPreviewRenderer preview3D;

    private File tempPreviewFile;
    private boolean isAlexSkin;
    private boolean applied;

    public SkinSearchByNickPanel(UIFormEditor editor)
    {
        super(L10n.lang("bbslezy.ui.skin.by_nickname"));
        this.editor = editor;

        UITextbox nickInput = new UITextbox(text -> this.nickname = text);
        nickInput.placeholder(L10n.lang("bbslezy.ui.skin.enter_nickname"));
        nickInput.relative(this.content).x(0.5F).y(8).w(220).h(20).anchor(0.5F);
        nickInput.resize();

        this.findButton = new UIButton(L10n.lang("bbslezy.ui.skin.find"), b -> this.searchPlayerSkin());
        this.findButton.relative(this.content).x(0.5F).y(32).w(220).h(20).anchor(0.5F);

        this.resultLabel = UI.label(L10n.lang("bbslezy.ui.skin.enter_nickname_prompt"));
        this.resultLabel.relative(this.content).x(0.5F).y(54).w(260).h(16).anchor(0.5F);

        this.previewRenderer = new UIRenderable(context ->
        {
            if (this.previewTexture != null)
            {
                int w = 80;
                int h = 80;
                int x = this.content.area.x + 15;
                int y = this.content.area.y + 72;
                int tw = this.previewTexture.width;
                int th = this.previewTexture.height;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.batcher.texturedBox(this.previewTexture, -1, (float) x, (float) y, (float) w, (float) h, 0.0F, 0.0F, (float) tw, (float) th, tw, th);
                RenderSystem.disableBlend();
            }
        });

        this.applyButton = new UIButton(L10n.lang("bbslezy.ui.skin.apply"), b -> this.applySkin());
        this.applyButton.relative(this.content).x(0.5F).y(232).w(220).h(20).anchor(0.5F);
        this.applyButton.setEnabled(false);

        this.content.add(nickInput, this.findButton, this.resultLabel, this.previewRenderer, this.applyButton);
        this.content.resize();
    }

    private void searchPlayerSkin()
    {
        String name = this.nickname.trim();

        if (!PlayerSkins.isNickname(name))
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
            this.applyButton.setEnabled(false);
            return;
        }

        LOG.info("Searching Mojang skin for: " + name);
        this.setResult(L10n.lang("bbslezy.ui.skin.searching"));
        this.clearPreview();
        this.applyButton.setEnabled(false);

        Link link = new Link(PlayerSkins.SOURCE, name + ".png");
        PlayerSkins.forget(name);

        PlayerSkins.request(link, name, loaded ->
        {
            if (!Boolean.TRUE.equals(loaded))
            {
                this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
                return;
            }

            File file = PlayerSkins.getFile(name);
            if (file == null || !file.exists())
            {
                this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
                return;
            }

            try
            {
                /* Save strictly to .temp folder for temporary preview */
                File tempFolder = new File(BBSMod.getAssetsFolder(), "skins/.temp");
                tempFolder.mkdirs();
                this.tempPreviewFile = new File(tempFolder, name + ".png");
                Files.copy(file.toPath(), this.tempPreviewFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                /* Create 2D texture preview */
                try (InputStream in = new FileInputStream(this.tempPreviewFile))
                {
                    Pixels pixels = Pixels.fromPNGStream(in);
                    if (pixels != null)
                    {
                        this.previewTexture = Texture.textureFromPixels(pixels, GL11.GL_NEAREST);
                    }
                }

                /* Create 3D rotatable preview using temp link */
                this.isAlexSkin = SkinFetcher.isAlex(this.tempPreviewFile);
                Link tempLink = Link.assets("skins/.temp/" + name + ".png");
                try
                {
                    BBSModClient.getTextures().delete(tempLink);
                }
                catch (Throwable ignored)
                {}

                this.clearPreview3D();
                this.preview3D = new SkinPreviewRenderer(tempLink, this.isAlexSkin);
                this.preview3D.relative(this.content).x(105).y(72).w(200).h(155);
                this.content.add(this.preview3D);
                this.content.resize();

                this.applyButton.setEnabled(true);
                this.setResult(L10n.lang("bbslezy.ui.skin.skin_found"));
            }
            catch (Exception e)
            {
                LOG.error("Failed to display skin preview for " + name, e);
                this.setResult(L10n.lang("bbslezy.ui.skin.preview_failed"));
            }
        });
    }

    private void applySkin()
    {
        String name = this.nickname.trim();
        if (this.tempPreviewFile == null || !this.tempPreviewFile.exists() || !PlayerSkins.isNickname(name))
        {
            return;
        }

        try
        {
            /* 1. Save permanently to BBS assets/skins folder */
            File skinsFolder = new File(BBSMod.getAssetsFolder(), "skins");
            skinsFolder.mkdirs();
            File dest = new File(skinsFolder, name + ".png");
            Files.copy(this.tempPreviewFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Link permanentLink = Link.assets("skins/" + name + ".png");
            BBSResources.markAssetsChanged();

            /* 2. Apply directly to the form/replay in UIFormEditor */
            if (this.editor != null)
            {
                Form current = this.editor.form;
                ModelForm modelForm;

                if (current instanceof ModelForm)
                {
                    modelForm = (ModelForm) current;
                }
                else
                {
                    modelForm = new ModelForm();
                }

                modelForm.texture.set(permanentLink);
                modelForm.model.set(this.isAlexSkin ? "player/alex" : "player/steve");
                this.editor.edit(modelForm);
            }

            this.applied = true;
            this.close();
        }
        catch (Exception e)
        {
            LOG.error("Failed to apply skin " + name, e);
            this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
        }
    }

    @Override
    public void close()
    {
        /* If user closes without applying, clean up the temporary preview file */
        if (!this.applied && this.tempPreviewFile != null && this.tempPreviewFile.exists())
        {
            try
            {
                this.tempPreviewFile.delete();
            }
            catch (Throwable ignored)
            {}
        }

        this.clearPreview();
        super.close();
    }

    private void setResult(IKey text)
    {
        this.resultLabel.label = text;
        this.resultLabel.resize();
        this.content.resize();
    }

    private void clearPreview()
    {
        this.previewTexture = null;
        this.clearPreview3D();
    }

    private void clearPreview3D()
    {
        if (this.preview3D != null)
        {
            this.preview3D.removeFromParent();
            this.preview3D = null;
        }
    }
}
