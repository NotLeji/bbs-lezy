package leji.bbslezy.ui.skin;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SkinSearchPanel extends UIOverlayPanel
{
    private static final Logger LOG = LogManager.getLogger("bbslezy");

    public SkinSearchPanel()
    {
        super(L10n.lang("bbslezy.ui.skin.search"));

        UIButton nickButton = new UIButton(L10n.lang("bbslezy.ui.skin.by_nickname"), b ->
        {
            LOG.info("Opening nickname panel...");
            UIContext context = this.getContext();
            if (context != null)
            {
                this.close();
                UIOverlay.addOverlay(context, new SkinSearchByNickPanel(), 280, 220);
            }
        });

        UIButton keywordButton = new UIButton(L10n.lang("bbslezy.ui.skin.by_keywords"), b ->
        {
            LOG.info("Opening keywords panel...");
            UIContext context = this.getContext();
            if (context != null)
            {
                this.close();
                UIOverlay.addOverlay(context, new SkinSearchByKeywordsPanel(), 820, 540);
            }
        });

        this.content.add(nickButton, keywordButton);
        nickButton.relative(this.content).x(0.5F).y(0.4F).w(200).h(20).anchor(0.5F);
        keywordButton.relative(this.content).x(0.5F).y(0.6F).w(200).h(20).anchor(0.5F);
        this.content.resize();
    }
}
