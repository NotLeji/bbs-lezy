package leji.bbslezy.ui.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSResources;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
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
import net.minecraft.client.MinecraftClient;
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
    private UIButton saveButton;
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

        this.saveButton = new UIButton(L10n.lang("bbslezy.ui.skin.save"), b -> this.saveSkinOnly());
        this.applyButton = new UIButton(L10n.lang("bbslezy.ui.skin.apply"), b -> this.applySkinToEditor());

        this.content.add(nickInput, this.findButton, this.resultLabel, this.previewRenderer, this.saveButton, this.applyButton);
        this.updateButtonsLayout(false);
    }

    /**
     * Determines whether the given form can receive a player skin.
     * Covers vanilla player models, replay actors, and all custom models/rigs (ModelForm),
     * as well as MobForm.
     */
    public static boolean isPlayerSkinCompatible(Form form)
    {
        if (form == null)
        {
            return false;
        }

        /* ModelForm covers vanilla player, replay actors, and all custom player rigs/models */
        if (form instanceof ModelForm)
        {
            return true;
        }

        /* MobForm can also use player skins via texture and slim flag */
        if (form instanceof MobForm)
        {
            return true;
        }

        return false;
    }

    private boolean canApplyToEditor()
    {
        return this.editor != null && isPlayerSkinCompatible(this.editor.form);
    }

    private void updateButtonsLayout(boolean enabled)
    {
        boolean canApply = this.canApplyToEditor();

        if (canApply)
        {
            /* Two buttons: [Save] and [Apply] */
            this.saveButton.relative(this.content).x(0.5F, -57).y(232).w(105).h(20).anchor(0.5F);
            this.saveButton.setVisible(true);
            this.saveButton.setEnabled(enabled);

            this.applyButton.relative(this.content).x(0.5F, 57).y(232).w(105).h(20).anchor(0.5F);
            this.applyButton.setVisible(true);
            this.applyButton.setEnabled(enabled);
        }
        else
        {
            /* Single button: [Save] spanning full width */
            this.saveButton.relative(this.content).x(0.5F).y(232).w(220).h(20).anchor(0.5F);
            this.saveButton.setVisible(true);
            this.saveButton.setEnabled(enabled);

            this.applyButton.setVisible(false);
            this.applyButton.setEnabled(false);
        }

        this.content.resize();
    }

    private void searchPlayerSkin()
    {
        String name = this.nickname.trim();

        if (!PlayerSkins.isNickname(name))
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
            this.updateButtonsLayout(false);
            return;
        }

        LOG.info("Searching Mojang skin for: " + name);
        this.setResult(L10n.lang("bbslezy.ui.skin.searching"));
        this.clearPreview();
        this.updateButtonsLayout(false);

        /* Fetch strictly to OS temporary folder - no Minecraft permanent storage touched */
        SkinFetcher.fetchMojangSkin(name, tempFile -> MinecraftClient.getInstance().execute(() ->
        {
            if (tempFile == null || !tempFile.exists())
            {
                this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
                return;
            }

            try
            {
                this.tempPreviewFile = tempFile;

                /* Create 2D texture preview */
                try (InputStream in = new FileInputStream(tempFile))
                {
                    Pixels pixels = Pixels.fromPNGStream(in);
                    if (pixels != null)
                    {
                        Link tempLink = Link.bbs("bbslezy_temp_skin_" + name.toLowerCase());
                        Texture texture = BBSModClient.getTextures().createTexture(tempLink);
                        texture.bind();
                        texture.uploadTexture(pixels);
                        this.previewTexture = texture;

                        /* Create 3D rotatable preview using temp link */
                        this.isAlexSkin = SkinFetcher.isAlex(tempFile);
                        this.clearPreview3D();
                        this.preview3D = new SkinPreviewRenderer(tempLink, this.isAlexSkin);
                        this.preview3D.relative(this.content).x(105).y(72).w(200).h(155);
                        this.content.add(this.preview3D);
                        this.content.resize();
                    }
                }

                this.updateButtonsLayout(true);
                this.setResult(L10n.lang("bbslezy.ui.skin.skin_found"));
            }
            catch (Exception e)
            {
                LOG.error("Failed to display skin preview for " + name, e);
                this.setResult(L10n.lang("bbslezy.ui.skin.preview_failed"));
            }
        }));
    }

    private boolean saveSkinToMinecraft(String name)
    {
        if (this.tempPreviewFile == null || !this.tempPreviewFile.exists() || !PlayerSkins.isNickname(name))
        {
            return false;
        }

        try
        {
            /* 1. Save to BBS assets/skins folder */
            File skinsFolder = new File(BBSMod.getAssetsFolder(), "skins");
            skinsFolder.mkdirs();
            File dest = new File(skinsFolder, name + ".png");
            Files.copy(this.tempPreviewFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            /* 2. Register into BBS "player" directory / player: source */
            Link playerLink = new Link(PlayerSkins.SOURCE, name + ".png");
            PlayerSkins.forget(name);
            PlayerSkins.request(playerLink, name, null);

            BBSResources.markAssetsChanged();
            return true;
        }
        catch (Exception e)
        {
            LOG.error("Failed to save skin " + name, e);
            return false;
        }
    }

    private void saveSkinOnly()
    {
        String name = this.nickname.trim();
        if (this.saveSkinToMinecraft(name))
        {
            this.applied = true;
            this.close();
        }
        else
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
        }
    }

    private void applySkinToEditor()
    {
        String name = this.nickname.trim();
        if (!this.saveSkinToMinecraft(name))
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
            return;
        }

        Link permanentLink = Link.assets("skins/" + name + ".png");

        if (this.canApplyToEditor())
        {
            Form form = this.editor.form;

            if (form instanceof ModelForm modelForm)
            {
                modelForm.texture.set(permanentLink);

                /* Only switch model to player/steve or player/alex if it's currently a vanilla player model.
                 * For custom models (e.g. custom player rigs), keep their custom model intact! */
                String currentModel = modelForm.model.get();
                if (currentModel == null || currentModel.isEmpty() || currentModel.startsWith("player"))
                {
                    modelForm.model.set(this.isAlexSkin ? "player/alex" : "player/steve");
                }

                this.editor.edit(modelForm);
            }
            else if (form instanceof MobForm mobForm)
            {
                mobForm.texture.set(permanentLink);
                mobForm.slim.set(this.isAlexSkin);
                this.editor.edit(mobForm);
            }
        }

        this.applied = true;
        this.close();
    }

    @Override
    public void close()
    {
        /* If user closes without applying/saving, delete the temporary preview file from OS temp */
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
