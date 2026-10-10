package leji.bbslezy.camera.clips.modifiers;

public enum ShakePreset
{
    GENTLE("Gentle"),
    ACTION("Action"),
    EXPLOSION("Explosion");

    private final String label;

    ShakePreset(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return this.label;
    }
}
