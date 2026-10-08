package leji.bbslezy.ui.skin;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.resources.Pixels;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

public class SkinSearchByNickPanel extends UIOverlayPanel
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");
    private static final String SKINS_FOLDER = "skins/";

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
        nickInput.relative(this.content).x(0.5F).y(0.05F).w(200).h(20).anchor(0.5F);
        nickInput.resize();

        UIButton findButton = new UIButton(L10n.lang("bbslezy.ui.skin.find"), b ->
        {
            LOG.info("Searching for: " + this.nickname);
            this.setResult(L10n.lang("bbslezy.ui.skin.searching"));
            this.clearPreview();

            SkinFetcher.checkSkinExists(this.nickname, exists -> MinecraftClient.getInstance().execute(() ->
            {
                if (!Boolean.TRUE.equals(exists))
                {
                    this.setResult(L10n.lang("bbslezy.ui.skin.not_found"));
                    return;
                }

                this.setResult(L10n.lang("bbslezy.ui.skin.downloading"));

                SkinFetcher.downloadSkin(this.nickname, file -> MinecraftClient.getInstance().execute(() ->
                {
                    if (file == null)
                    {
                        this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
                        return;
                    }

                    try
                    {
                        Link link = Link.assets(SKINS_FOLDER + this.nickname + ".png");
                        BBSModClient.getTextures().delete(link);
                        Pixels pixels = BBSModClient.getTextures().getPixels(link);
                        if (pixels == null)
                        {
                            this.setResult(L10n.lang("bbslezy.ui.skin.failed_read_pixels"));
                            return;
                        }

                        this.previewTexture = Texture.textureFromPixels(pixels, GL11.GL_NEAREST);
                        LOG.info("Manual texture: id=" + this.previewTexture.id + " size " + this.previewTexture.width + "x" + this.previewTexture.height);

                        this.clearPreview3D();
                        this.preview3D = new SkinPreviewRenderer(SKINS_FOLDER + this.nickname, false);
                        this.preview3D.relative(this.content).x(90).y(45).w(200).h(200);
                        this.content.add(this.preview3D);
                        this.content.resize();

                        this.setResult(L10n.lang("bbslezy.ui.skin.skin_found"));
                    }
                    catch (Exception e)
                    {
                        LOG.error("Preview failed", e);
                        this.setResult(L10n.lang("bbslezy.ui.skin.preview_failed"));
                    }
                }));
            }));
        });
        findButton.relative(this.content).x(0.5F).y(0.2F).w(200).h(20).anchor(0.5F);

        this.resultLabel = UI.label(L10n.lang("bbslezy.ui.skin.enter_nickname_prompt"));
        this.resultLabel.relative(this.content).x(0.5F).y(0.35F).w(200).h(20).anchor(0.5F);

        this.previewRenderer = new UIRenderable(context ->
        {
            if (this.previewTexture != null)
            {
                int w = 80;
                int h = 80;
                int x = this.content.area.x + 20;
                int y = this.content.area.y + 100;
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
