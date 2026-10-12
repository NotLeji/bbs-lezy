package leji.bbslezy.vfx.mixin;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayPropertiesPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import leji.bbslezy.vfx.client.LezyVfxUI;
import leji.bbslezy.vfx.forms.CurveForm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adds a "Follow curve" section to the per-actor Replay settings panel.
 */
@Mixin(value = UIReplayPropertiesPanel.class, remap = false)
public abstract class UIReplayPropertiesPanelMixin
{
    @Shadow public UIElement properties;
    @Shadow @Final private UIFilmPanel filmPanel;

    @Shadow private void edit(Consumer<Replay> consumer)
    {
    }

    @Unique private UIToggle bbslezy$followEnabled;
    @Unique private UIButton bbslezy$followCurve;
    @Unique private UIToggle bbslezy$followAlign;
    @Unique private int bbslezy$target = -1;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bbslezy$addFollowSection(UIFilmPanel filmPanel, CallbackInfo ci)
    {
        this.bbslezy$followEnabled = new UIToggle(IKey.constant("Enabled"),
            (b) -> this.edit((replay) -> this.bbslezy$setBool(replay, "xavin_follow_enabled", b.getValue())));
        this.bbslezy$followCurve = new UIButton(IKey.constant("(no curve)"), (b) -> this.bbslezy$cycleCurve());
        this.bbslezy$followCurve.tooltip(IKey.constant("Curve actor to follow (cycles through curve actors)"));
        this.bbslezy$followAlign = new UIToggle(IKey.constant("Align to curve"),
            (b) -> this.edit((replay) -> this.bbslezy$setBool(replay, "xavin_follow_align", b.getValue())));

        this.properties.add(LezyVfxUI.section(IKey.constant("Follow curve"),
            this.bbslezy$followEnabled, this.bbslezy$followCurve, this.bbslezy$followAlign));
    }

    @Inject(method = "setReplay", at = @At("TAIL"))
    private void bbslezy$syncFollowSection(Replay replay, CallbackInfo ci)
    {
        if (replay == null)
        {
            return;
        }

        if (replay.get("xavin_follow_enabled") instanceof ValueBoolean enabled)
        {
            this.bbslezy$followEnabled.setValue(enabled.get());
        }

        if (replay.get("xavin_follow_align") instanceof ValueBoolean align)
        {
            this.bbslezy$followAlign.setValue(align.get());
        }

        this.bbslezy$target = replay.get("xavin_follow_target") instanceof ValueInt target ? target.get() : -1;
        this.bbslezy$followCurve.label = IKey.constant(this.bbslezy$curveLabel(this.bbslezy$target));
    }

    @Unique
    private void bbslezy$cycleCurve()
    {
        List<Integer> curves = this.bbslezy$curveActors();

        if (curves.isEmpty())
        {
            return;
        }

        int idx = curves.indexOf(this.bbslezy$target);
        int next = curves.get((idx + 1) % curves.size());

        this.bbslezy$target = next;
        this.edit((replay) ->
        {
            if (replay.get("xavin_follow_target") instanceof ValueInt target)
            {
                target.set(next);
            }
        });
        this.bbslezy$followCurve.label = IKey.constant(this.bbslezy$curveLabel(next));
    }

    @Unique
    private List<Integer> bbslezy$curveActors()
    {
        List<Integer> out = new ArrayList<>();
        Film film = this.filmPanel.getData();

        if (film != null)
        {
            List<Replay> replays = film.replays.getList();

            for (int i = 0; i < replays.size(); i++)
            {
                if (replays.get(i).form.get() instanceof CurveForm)
                {
                    out.add(i);
                }
            }
        }

        return out;
    }

    @Unique
    private String bbslezy$curveLabel(int index)
    {
        Film film = this.filmPanel.getData();

        if (film == null || index < 0 || index >= film.replays.getList().size())
        {
            return "(no curve)";
        }

        String label = film.replays.getList().get(index).label.get();

        return (label == null || label.isEmpty() ? "Curve" : label) + " #" + index;
    }

    @Unique
    private void bbslezy$setBool(Replay replay, String key, boolean value)
    {
        if (replay.get(key) instanceof ValueBoolean v)
        {
            v.set(value);
        }
    }
}
