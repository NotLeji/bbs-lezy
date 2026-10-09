package leji.bbslezy.camera.clips.screen;

public enum AspectRatioPreset
{
    SCOPE_239("2.39:1 (Scope)", 2.39F),
    CINEMA_235("2.35:1 (Cinema)", 2.35F),
    UNIVISIUM_200("2.00:1 (Univisium)", 2.0F),
    FLAT_185("1.85:1 (Flat)", 1.85F),
    HD_169("16:9 (HD)", 16F / 9F),
    ACADEMY_43("4:3 (Academy)", 4F / 3F),
    SQUARE_11("1:1 (Square)", 1F),
    SHORTS_916("9:16 (Shorts)", 9F / 16F);

    private final String label;
    private final float ratio;

    AspectRatioPreset(String label, float ratio)
    {
        this.label = label;
        this.ratio = ratio;
    }

    public String getLabel()
    {
        return this.label;
    }

    public float getRatio()
    {
        return this.ratio;
    }
}
