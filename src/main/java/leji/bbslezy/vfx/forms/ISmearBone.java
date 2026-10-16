package leji.bbslezy.vfx.forms;

/**
 * Duck interface mixed onto BBS's {@code PoseTransform} so every pose bone carries its own smear: a
 * vector (manual direction × length) plus the look knobs (copy count, opacity falloff). Because they
 * live in {@code PoseTransform} they serialise and interpolate with the pose — so the smear animates
 * like any other pose channel. {@code count}/{@code falloff} of 0 mean "use the render defaults".
 */
public interface ISmearBone
{
    float bbslezy$smearX();

    float bbslezy$smearY();

    float bbslezy$smearZ();

    void bbslezy$setSmear(float x, float y, float z);

    float bbslezy$smearCount();

    void bbslezy$setSmearCount(float count);

    float bbslezy$smearFalloff();

    void bbslezy$setSmearFalloff(float falloff);

    float bbslezy$smearDissolve();

    void bbslezy$setSmearDissolve(float dissolve);

    /** Manual mode enable: &gt; 0 = the straight-vector smear is active (separate from the vector values, so
     *  toggling the mode off stops the smear without losing the configured vector). */
    float bbslezy$smearManual();

    void bbslezy$setSmearManual(float manual);

    /**
     * Auto-arc mode: 0 = off (the straight-vector smear above), &gt; 0 = the echo copies follow the
     * bone's REAL motion arc — each copy is the model posed at an earlier moment in time
     * (time-rewound), so a swinging limb leaves a curved trail instead of a straight one.
     */
    float bbslezy$smearArc();

    void bbslezy$setSmearArc(float arc);

    /** Trail length in ticks for arc mode (how far back in time the echo reaches). 0 = render default. */
    float bbslezy$smearTime();

    void bbslezy$setSmearTime(float time);

    /**
     * Arc-copy deformation strength (multiplier). Each echo copy is stretched along its motion and
     * squashed across (volume-preserving), growing toward the tail where copies also shrink + wash out
     * into abstract smears. 0 = a sensible default; the effect also scales with the bone's actual speed.
     */
    float bbslezy$smearStretch();

    void bbslezy$setSmearStretch(float stretch);

    /**
     * Overlap density (arc mode): how many faint, deformed in-between copies are rendered along the
     * trajectory between the crisp multiples. Higher = the stretched copies overlap into a continuous
     * smear (closes the gaps between the fanned copies). 0 = render default.
     */
    float bbslezy$smearDensity();

    void bbslezy$setSmearDensity(float density);

    /** Opacity of each faint overlap copy in the band (arc mode). 0 = render default (~0.14). */
    float bbslezy$smearOpacity();

    void bbslezy$setSmearOpacity(float opacity);

    /** Motion lines: 0 = off, else opacity/intensity. */
    float bbslezy$smearLines();

    void bbslezy$setSmearLines(float lines);

    float bbslezy$smearLinesCount();

    void bbslezy$setSmearLinesCount(float count);

    float bbslezy$smearLinesWidth();

    void bbslezy$setSmearLinesWidth(float width);

    float bbslezy$smearLinesSpread();

    void bbslezy$setSmearLinesSpread(float spread);

    float bbslezy$smearLinesOffsetX();

    float bbslezy$smearLinesOffsetY();

    float bbslezy$smearLinesOffsetZ();

    void bbslezy$setSmearLinesOffset(float x, float y, float z);

    /**
     * Motion lines colour source: 0 = use the bone's pose colour (the editor's Color picker), &gt; 0 =
     * tint the lines with the actor's texture average instead. The line colour itself reuses the
     * built-in per-bone {@code PoseTransform.color}, so it serialises and animates with the pose.
     */
    float bbslezy$smearLinesTexture();

    void bbslezy$setSmearLinesTexture(float texture);

    default boolean bbslezy$hasSmear()
    {
        return this.bbslezy$smearX() != 0F || this.bbslezy$smearY() != 0F || this.bbslezy$smearZ() != 0F
            || this.bbslezy$smearArc() > 0F || this.bbslezy$smearManual() > 0F;
    }
}
