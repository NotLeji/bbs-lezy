package leji.bbslezy.camera.clips.screen;

public enum ImpactStyle
{
    ANIME_IMPACT("Anime Impact", true, 3, true, 0xFFFFFF, 1.0F, 4, true, 15.0F, 10, true, 1.5F, 8),
    HARD_HIT("Hard Hit", true, 2, true, 0xFFFFFF, 0.8F, 2, true, 8.0F, 6, true, 2.0F, 6),
    EXPLOSION("Explosion", false, 0, true, 0xFFAA22, 1.0F, 6, true, 20.0F, 12, true, 3.0F, 16),
    DRAMATIC("Dramatic", true, 6, false, 0xFFFFFF, 0F, 0, true, 12.0F, 16, true, 0.5F, 4),
    CUSTOM("Custom", false, 0, false, 0, 0F, 0, false, 0F, 0, false, 0F, 0);

    private final String label;
    public final boolean freeze;
    public final int freezeDuration;
    public final boolean flash;
    public final int flashColor;
    public final float flashIntensity;
    public final int flashDuration;
    public final boolean punchIn;
    public final float punchInFov;
    public final int punchInDuration;
    public final boolean shake;
    public final float shakeIntensity;
    public final int shakeDuration;

    ImpactStyle(String label, boolean freeze, int freezeDuration,
        boolean flash, int flashColor, float flashIntensity, int flashDuration,
        boolean punchIn, float punchInFov, int punchInDuration,
        boolean shake, float shakeIntensity, int shakeDuration)
    {
        this.label = label;
        this.freeze = freeze;
        this.freezeDuration = freezeDuration;
        this.flash = flash;
        this.flashColor = flashColor;
        this.flashIntensity = flashIntensity;
        this.flashDuration = flashDuration;
        this.punchIn = punchIn;
        this.punchInFov = punchInFov;
        this.punchInDuration = punchInDuration;
        this.shake = shake;
        this.shakeIntensity = shakeIntensity;
        this.shakeDuration = shakeDuration;
    }

    public String getLabel()
    {
        return this.label;
    }
}
