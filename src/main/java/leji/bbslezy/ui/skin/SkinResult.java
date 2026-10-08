package leji.bbslezy.ui.skin;

public class SkinResult
{
    public final String id;
    public final String title;
    public final String previewUrl;
    public final String downloadUrl;
    public final String pageUrl;

    public SkinResult(String id, String title, String previewUrl, String downloadUrl, String pageUrl)
    {
        this.id = id;
        this.title = title;
        this.previewUrl = previewUrl;
        this.downloadUrl = downloadUrl;
        this.pageUrl = pageUrl;
    }
}
