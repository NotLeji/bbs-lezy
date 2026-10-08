package leji.bbslezy.ui.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
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

    private String nickname = "";
    private UILabel resultLabel;
    private Texture previewTexture;
    private UIRenderable previewRenderer;
    private SkinPreviewRenderer preview3D;

    public SkinSearchByNickPanel()
    {
        super(L10n.lang("bbslezy.ui.skin.by_nickname"));

        UITextbox nickInput = new UITextbox(text -> this.nickname = text);
        nickInput.placeholder(L10n.lang("bbslezy.ui.skin.enter_nickname"));
        nickInput.relative(this.content).x(0.5F).y(8).w(220).h(20).anchor(0.5F);
        nickInput.resize();

        UIButton findButton = new UIButton(L10n.lang("bbslezy.ui.skin.find"), b -> this.searchPlayerSkin());
        findButton.relative(this.content).x(0.5F).y(32).w(220).h(20).anchor(0.5F);

        this.resultLabel = UI.label(L10n.lang("bbslezy.ui.skin.enter_nickname_prompt"));
        this.resultLabel.relative(this.content).x(0.5F).y(56).w(260).h(16).anchor(0.5F);

        this.previewRenderer = new UIRenderable(context ->
        {
            if (this.previewTexture != null)
            {
                int w = 80;
                int h = 80;
                int x = this.content.area.x + 15;
                int y = this.content.area.y + 75;
                int tw = this.previewTexture.width;
                int th = this.previewTexture.height;

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.batcher.texturedBox(this.previewTexture, -1, (float) x, (float) y, (float) w, (float) h, 0.0F, 0.0F, (float) tw, (float) th, tw, th);
                RenderSystem.disableBlend();
            }
        });

        this.content.add(nickInput, findButton, this.resultLabel, this.previewRenderer);
        this.content.resize();
    }

    private void searchPlayerSkin()
    {
        String name = this.nickname.trim();

        if (!PlayerSkins.isNickname(name))
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
            return;
        }

        LOG.info("Searching Mojang skin for: " + name);
        this.setResult(L10n.lang("bbslezy.ui.skin.searching"));
        this.clearPreview();

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
                /* Copy to project assets folder so it is available locally */
                File skinsFolder = new File(BBSMod.getAssetsFolder(), "skins");
                skinsFolder.mkdirs();
                File dest = new File(skinsFolder, name + ".png");
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                /* Create 2D texture preview */
                try (InputStream in = new FileInputStream(file))
                {
                    Pixels pixels = Pixels.fromPNGStream(in);
                    if (pixels != null)
                    {
                        this.previewTexture = Texture.textureFromPixels(pixels, GL11.GL_NEAREST);
                    }
                }

                /* Create 3D rotatable preview */
                boolean isAlex = SkinFetcher.isAlex(file);
                this.clearPreview3D();
                this.preview3D = new SkinPreviewRenderer(link, isAlex);
                this.preview3D.relative(this.content).x(105).y(75).w(200).h(170);
                this.content.add(this.preview3D);
                this.content.resize();

                this.setResult(L10n.lang("bbslezy.ui.skin.skin_found"));
            }
            catch (Exception e)
            {
                LOG.error("Failed to display skin preview for " + name, e);
                this.setResult(L10n.lang("bbslezy.ui.skin.preview_failed"));
            }
        });
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
