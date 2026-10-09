package leji.bbslezy.camera.clips.screen;

public enum TransitionType
{
    FADE_OUT("Fade Out"),
    FADE_IN("Fade In"),
    DIP_TO_BLACK("Dip to Black"),
    FLASH("Flash"),
    CROSSFADE("Crossfade");

    private final String label;

    TransitionType(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return this.label;
    }
}
