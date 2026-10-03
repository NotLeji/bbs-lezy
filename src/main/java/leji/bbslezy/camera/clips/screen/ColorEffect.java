package leji.bbslezy.camera.clips.screen;

public class ColorEffect implements LayeredEffect
{
    public int layer;
    public int renderOrder;

    @Override
    public int layer()
    {
        return this.layer;
    }

    @Override
    public int renderOrder()
    {
        return this.renderOrder;
    }

    public boolean hasOverlay;
    public int overlayColor;

    public boolean hasVignette;
    public int vignetteColor;
    public float vignetteStrength;
    public float vignetteSmoothness;

    public boolean hasDistort;
    public float distortX;
    public float distortY;

    public boolean hasGrade;
    public float brightness;
    public float contrast;
    public float saturation;
    public float hue;
    public float liftR, liftG, liftB;
    public float gammaR, gammaG, gammaB;
    public float gainR, gainG, gainB;

    public boolean hasCinematic;
    public float chromaticAberration;
    public float chromaticAberrationCenterX;
    public float chromaticAberrationCenterY;
    public float vhs;
    public float lensDistortion;
    public float lensOverscan;
    public float lensRadiusX;
    public float lensRadiusY;
    public float lensHardness;
    public float lensSharpen;
    public float lensCenterX;
    public float lensCenterY;
    public float vintage;
    public float radialBlur;
    public float radialBlurCenterX;
    public float radialBlurCenterY;
    public float rain;
    public float dust;
    public float lightLeak;
    public float lightLeakCenterX;
    public float lightLeakCenterY;
    public float heatStrength;
    public float heatSpeed;
    public float heatScale;
    public float time;
    public float pixelation;
    public float dof;
    public float dofFocus;
    public float dofBlur;

    public void reset()
    {
        this.layer = 0;
        this.renderOrder = 0;
        this.hasOverlay = false;
        this.hasVignette = false;
        this.hasGrade = false;
        this.hasDistort = false;
        this.hasCinematic = false;

        this.chromaticAberration = 0F;
        this.chromaticAberrationCenterX = 0.5F;
        this.chromaticAberrationCenterY = 0.5F;
        this.vhs = 0F;
        this.lensDistortion = 0F;
        this.lensOverscan = 1F;
        this.lensRadiusX = 1F;
        this.lensRadiusY = 1F;
        this.lensHardness = 1F;
        this.lensSharpen = 0F;
        this.lensCenterX = 0.5F;
        this.lensCenterY = 0.5F;
        this.vintage = 0F;
        this.radialBlur = 0F;
        this.radialBlurCenterX = 0.5F;
        this.radialBlurCenterY = 0.5F;
        this.rain = 0F;
        this.dust = 0F;
        this.lightLeak = 0F;
        this.lightLeakCenterX = 0.0F;
        this.lightLeakCenterY = 0.4F;
        this.heatStrength = 0F;
        this.heatSpeed = 0F;
        this.heatScale = 0F;
        this.time = 0F;
        this.pixelation = 0F;
        this.dof = 0F;
        this.dofFocus = 8.0F;
        this.dofBlur = 1.0F;
    }
}
