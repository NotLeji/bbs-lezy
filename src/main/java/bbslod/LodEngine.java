package bbslod;

import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.api.client.events.FormRenderEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Caps how many of a scene's forms get drawn per frame with focus-distance support.
 *
 * <p>Uses spatial deadband (only re-ranks when the camera moves significantly or settings change)
 * and hysteresis buffering so forms on the distance boundary do not flicker or thrash ("refresh")
 * during camera movement.</p>
 */
public class LodEngine
{
    /** Forms currently culled via {@code visible.setRuntimeValue(Boolean.FALSE)}. */
    private static final Set<Form> touched = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final List<Candidate> candidates = new ArrayList<>();

    private static double lastCamX = Double.NaN;
    private static double lastCamY = Double.NaN;
    private static double lastCamZ = Double.NaN;
    private static double lastFocus = -1D;
    private static int lastLimit = -1;
    private static boolean lastEnabled = false;
    private static int lastEntityCount = -1;
    private static int framesSinceUpdate = 0;
    private static boolean active = false;

    /** Deadband distance squared: ~0.6 blocks. Tiny camera jitters do not trigger re-ranking. */
    private static final double MOVE_THRESHOLD_SQ = 0.36D;

    /** Hysteresis buffer in blocks. Forms currently visible stay visible unless overtaken by this margin. */
    private static final double HYSTERESIS_MARGIN = 2.0D;

    /** Minimum frames between camera-motion re-rankings to maintain steady frame pacing. */
    private static final int MIN_INTERVAL_FRAMES = 3;

    /** Maximum stale frames before refreshing when moving actors might cross the focus window. */
    private static final int MAX_STALE_FRAMES = 15;

    public static void register()
    {
        FormRenderEvents.BEFORE.register(LodEngine::before);
        FormRenderEvents.AFTER.register(LodEngine::after);
        FilmEvents.RENDER_AFTER.register(LodEngine::onRenderAfter);
        FilmEvents.SHUTDOWN.register(LodEngine::onShutdown);
    }

    /**
     * Stencil picking pass needs to click culled actors: temporarily unhide on picking BEFORE,
     * then restore in AFTER. During normal world rendering, does nothing.
     */
    private static void before(Form form, FormRenderingContext context)
    {
        if (context.isPicking() && touched.contains(form))
        {
            form.visible.setRuntimeValue(null);
        }
    }

    private static void after(Form form, FormRenderingContext context)
    {
        if (context.isPicking() && touched.contains(form))
        {
            form.visible.setRuntimeValue(Boolean.FALSE);
        }
    }

    /**
     * Ranks this controller's forms by distance/focus to the camera with deadband and hysteresis.
     */
    private static void onRenderAfter(BaseFilmController controller, WorldRenderContext context)
    {
        boolean enabled = LodSettings.enabled.get();
        int limit = LodSettings.renderLimit.get();

        if (!enabled || limit <= 0)
        {
            if (active)
            {
                clearOverrides(controller);
                reset();
            }
            return;
        }

        active = true;

        Vec3d camera = context.camera().getPos();
        double focus = LodSettings.focusDistance.get();
        int entityCount = controller.getEntities().size();

        double dx = camera.x - lastCamX;
        double dy = camera.y - lastCamY;
        double dz = camera.z - lastCamZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        boolean settingsChanged = enabled != lastEnabled || limit != lastLimit || focus != lastFocus || entityCount != lastEntityCount;
        boolean moved = distSq >= MOVE_THRESHOLD_SQ;
        boolean stale = framesSinceUpdate >= MAX_STALE_FRAMES;

        framesSinceUpdate++;

        /* Skip re-ranking if camera hasn't moved beyond deadband and settings haven't changed */
        if (!settingsChanged)
        {
            if (!moved && !stale)
            {
                return;
            }
            if (framesSinceUpdate < MIN_INTERVAL_FRAMES)
            {
                return;
            }
        }

        framesSinceUpdate = 0;
        lastCamX = camera.x;
        lastCamY = camera.y;
        lastCamZ = camera.z;
        lastFocus = focus;
        lastLimit = limit;
        lastEnabled = enabled;
        lastEntityCount = entityCount;

        int count = 0;
        for (IEntity entity : controller.getEntities().values())
        {
            Form form = entity.getForm();

            if (form == null || form.anchor.get().hasTarget())
            {
                continue;
            }

            double ex = entity.getX() - camera.x;
            double ey = entity.getY() - camera.y;
            double ez = entity.getZ() - camera.z;
            double dist = Math.sqrt(ex * ex + ey * ey + ez * ez);
            double score = focus > 0D ? Math.abs(dist - focus) : dist;

            /* Hysteresis: forms already visible get a margin bonus to prevent edge flickering */
            double sortScore = touched.contains(form) ? score : score - HYSTERESIS_MARGIN;

            if (count < candidates.size())
            {
                candidates.get(count).set(form, sortScore);
            }
            else
            {
                candidates.add(new Candidate(form, sortScore));
            }
            count++;
        }

        while (candidates.size() > count)
        {
            candidates.remove(candidates.size() - 1);
        }

        candidates.sort(null);

        /* Apply visibility state diff: only change runtime value when visibility changes */
        for (int i = 0; i < count; i++)
        {
            Candidate c = candidates.get(i);
            Form form = c.form;

            if (i < limit)
            {
                if (touched.remove(form))
                {
                    form.visible.setRuntimeValue(null);
                }
            }
            else
            {
                if (touched.add(form))
                {
                    form.visible.setRuntimeValue(Boolean.FALSE);
                }
            }
        }

        /* Clean up any forms in touched that are no longer in candidates */
        if (touched.size() > count)
        {
            Set<Form> valid = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int i = 0; i < count; i++)
            {
                valid.add(candidates.get(i).form);
            }
            touched.retainAll(valid);
        }
    }

    private static void onShutdown(BaseFilmController controller)
    {
        clearOverrides(controller);
        reset();
    }

    private static void reset()
    {
        lastCamX = Double.NaN;
        lastCamY = Double.NaN;
        lastCamZ = Double.NaN;
        lastFocus = -1D;
        lastLimit = -1;
        lastEntityCount = -1;
        lastEnabled = false;
        framesSinceUpdate = 0;
        active = false;
    }

    /**
     * Clears every runtime override the controller's root forms carry.
     */
    private static void clearOverrides(BaseFilmController controller)
    {
        if (controller != null)
        {
            for (IEntity entity : controller.getEntities().values())
            {
                Form form = entity.getForm();

                if (form != null)
                {
                    form.visible.setRuntimeValue(null);
                }
            }
        }
        else
        {
            for (Form form : touched)
            {
                form.visible.setRuntimeValue(null);
            }
        }

        touched.clear();
    }

    private static class Candidate implements Comparable<Candidate>
    {
        public Form form;
        public double score;

        public Candidate(Form form, double score)
        {
            this.form = form;
            this.score = score;
        }

        public void set(Form form, double score)
        {
            this.form = form;
            this.score = score;
        }

        @Override
        public int compareTo(Candidate other)
        {
            return Double.compare(this.score, other.score);
        }
    }
}
