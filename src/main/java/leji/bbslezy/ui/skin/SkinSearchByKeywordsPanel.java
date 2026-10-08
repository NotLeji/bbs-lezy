package leji.bbslezy.ui.skin;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SkinSearchByKeywordsPanel extends UIOverlayPanel
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");
    private static final int COLS = 3;
    private static final int CARDS_PER_PAGE = 6;

    private String keyword = "";
    private UILabel resultLabel;
    private UIButton findButton;
    private final List<UIElement> cardElements = new ArrayList<>();
    private final List<UIButton> pageButtons = new ArrayList<>();
    private final List<SkinResult> allResults = new ArrayList<>();
    private int currentPage = 0;

    public SkinSearchByKeywordsPanel()
    {
        super(L10n.lang("bbslezy.ui.skin.by_keywords"));

        SkinFetcher.cleanupAllPreviews();

        UITextbox keywordInput = new UITextbox(text -> this.keyword = text);
        keywordInput.placeholder(L10n.lang("bbslezy.ui.skin.enter_keyword"));
        keywordInput.relative(this.content).x(0.5F).y(6).w(400).h(20).anchor(0.5F);

        this.findButton = new UIButton(L10n.lang("bbslezy.ui.skin.find"), b -> this.performSearch());
        this.findButton.relative(this.content).x(0.5F).y(32).w(400).h(20).anchor(0.5F);

        this.resultLabel = UI.label(L10n.lang("bbslezy.ui.skin.enter_keyword_prompt"));
        this.resultLabel.relative(this.content).x(0.5F).y(58).w(600).h(18).anchor(0.5F);

        this.content.add(keywordInput, this.findButton, this.resultLabel);
        this.content.resize();
    }

    @Override
    public void close()
    {
        LOG.info("Closing keywords panel");
        this.clearCards();
        this.clearPageButtons();
        SkinFetcher.cleanupAllPreviews();
        super.close();
    }

    private void performSearch()
    {
        if (this.keyword == null || this.keyword.trim().isEmpty())
        {
            this.setResult(L10n.lang("bbslezy.ui.skin.enter_keyword"));
            return;
        }

        this.clearCards();
        this.clearPageButtons();
        SkinFetcher.cleanupAllPreviews();
        this.allResults.clear();
        this.currentPage = 0;
        this.setResult(L10n.lang("bbslezy.ui.skin.searching"));

        SkinFetcher.searchSkinsByKeyword(this.keyword, results -> MinecraftClient.getInstance().execute(() ->
        {
            if (results == null || results.isEmpty())
            {
                this.setResult(L10n.lang("bbslezy.ui.skin.no_results"));
                return;
            }

            this.allResults.addAll(results);
            int totalPages = (results.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
            this.setResult("Found: " + results.size() + " skins, " + totalPages + " pages");
            this.buildPageButtons();
            this.showPage(0);
        }));
    }

    private void clearCards()
    {
        for (UIElement e : this.cardElements)
        {
            e.removeFromParent();
        }
        this.cardElements.clear();
    }

    private void clearPageButtons()
    {
        for (UIButton b : this.pageButtons)
        {
            b.removeFromParent();
        }
        this.pageButtons.clear();
    }

    private void buildPageButtons()
    {
        this.clearPageButtons();
        int totalPages = (this.allResults.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
        if (totalPages <= 1)
        {
            return;
        }

        int btnW = 32;
        int btnH = 20;
        int gap = 4;
        int totalW = totalPages * btnW + (totalPages - 1) * gap;
        int startX = (this.content.area.w - totalW) / 2;
        int y = 490;

        for (int i = 0; i < totalPages; i++)
        {
            int page = i;
            UIButton btn = new UIButton(IKey.raw(String.valueOf(i + 1)), b -> this.showPage(page));
            btn.relative(this.content).x(startX + i * (btnW + gap)).y(y).w(btnW).h(btnH);
            this.content.add(btn);
            this.pageButtons.add(btn);
        }
        this.content.resize();
    }

    private void showPage(int page)
    {
        this.currentPage = page;
        this.clearCards();
        SkinFetcher.cleanupAllPreviews();

        int start = page * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, this.allResults.size());
        int count = end - start;

        for (int i = 0; i < count; i++)
        {
            SkinResult result = this.allResults.get(start + i);
            int slot = i;
            int col = slot % COLS;
            int row = slot / COLS;

            SkinFetcher.downloadPreview(result.downloadUrl, result.id, file ->
            {
                if (file == null)
                {
                    LOG.warn("Preview download failed for id=" + result.id);
                    return;
                }

                boolean valid = SkinFetcher.isValidSkin(file);
                LOG.info("VALID check for id=" + result.id + " (" + result.title + "): " + valid);
                if (!valid)
                {
                    LOG.warn("  SKIPPED (broken): " + result.title);
                    try
                    {
                        file.delete();
                    }
                    catch (Exception ignored)
                    {}
                    return;
                }

                boolean isAlex = SkinFetcher.isAlex(file);
                MinecraftClient.getInstance().execute(() -> this.addCard(result, col, row, isAlex));
            });
        }
    }

    private void addCard(SkinResult result, int col, int row, boolean isAlex)
    {
        int cardW = 250;
        int titleH = 18;
        int previewH = 130;
        int buttonH = 20;
        int gapX = 10;
        int gapY = 8;
        int startX = 15;
        int startY = 85;
        int rowH = titleH + previewH + buttonH + gapY;
        int x = startX + col * (cardW + gapX);
        int y = startY + row * rowH;

        String previewPath = "skins/.temp/" + result.id;
        try
        {
            Link previewLink = Link.assets(previewPath + ".png");
            BBSModClient.getTextures().delete(previewLink);
        }
        catch (Exception ignored)
        {}

        String titleText = result.title;
        if (titleText.length() > 28)
        {
            titleText = titleText.substring(0, 28) + "...";
        }

        UILabel title = UI.label(IKey.raw(titleText));
        title.relative(this.content).x(x).y(y).w(cardW).h(titleH);

        SkinPreviewRenderer preview = new SkinPreviewRenderer(previewPath, isAlex);
        preview.relative(this.content).x(x).y(y + titleH).w(cardW).h(previewH);

        UIButton downloadBtn = new UIButton(L10n.lang("bbslezy.ui.skin.download"), b ->
        {
            String filename = this.sanitize(result.title);
            this.setResult("Downloading: " + result.title);
            SkinFetcher.downloadSkinByUrl(result.downloadUrl, filename, file -> MinecraftClient.getInstance().execute(() ->
            {
                if (file != null)
                {
                    this.setResult("Saved: " + file.getName());
                }
                else
                {
                    this.setResult(L10n.lang("bbslezy.ui.skin.download_failed"));
                }
            }));
        });
        downloadBtn.relative(this.content).x(x + 15).y(y + titleH + previewH + 2).w(cardW - 30).h(buttonH);

        this.content.add(title, preview, downloadBtn);
        this.content.resize();
        this.cardElements.add(title);
        this.cardElements.add(preview);
        this.cardElements.add(downloadBtn);
    }

    static String sanitize(String s)
    {
        if (s == null || s.isEmpty())
        {
            return "skin";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray())
        {
            if (Character.isLetterOrDigit(c))
            {
                sb.append(c);
                continue;
            }
            if (c != ' ' && c != '_' && c != '-')
            {
                continue;
            }
            sb.append('_');
        }
        String out = sb.toString();
        if (out.isEmpty())
        {
            out = "skin";
        }
        if (out.length() > 40)
        {
            out = out.substring(0, 40);
        }
        return out;
    }

    private void setResult(String text)
    {
        this.setResult(IKey.raw(text));
    }

    private void setResult(IKey key)
    {
        this.resultLabel.label = key;
        this.resultLabel.resize();
        this.content.resize();
    }
}
