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
 * Caps how many of a scene's forms get drawn per frame: the closest ones to the camera win, the
 * rest are held back, so a scene with thousands of actors costs a bounded amount of GPU while the
 * scene is being built.
 *
 * <p>The ranking is rebuilt once per frame in {@link FilmEvents#RENDER_AFTER} — after the frame
 * drew, when every form's distance is known — and consumed by the next frame's {@code BEFORE}.
 * That is one frame behind the camera, which is a fine price for a full sort: a form walking into
 * range appears a frame late rather than the whole budget being spent on whichever forms the
 * iteration happened to reach first.</p>
 *
 * <p>Hiding goes through {@code visible}'s runtime value for the same reason BBS's own keyframes
 * do — it never clobbers a track the user wrote. Picking passes are skipped, so editor clicks
 * still select an actor the cap is currently holding back.</p>
 */
public class LodEngine
{
    /** Forms whose {@code visible} runtime value this engine set, so it can put them back. */
    private static final Set<Form> touched = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final List<Candidate> candidates = new ArrayList<>();
    private static double budget;
    private static boolean budgetValid;

    public static void register()
    {
        FormRenderEvents.BEFORE.register(LodEngine::before);
        FilmEvents.RENDER_AFTER.register(LodEngine::onRenderAfter);
        FilmEvents.SHUTDOWN.register(LodEngine::onShutdown);
    }

    private static void before(Form form, FormRenderingContext context)
    {
        if (!budgetValid || !LodSettings.enabled.get() || !form.visible.get())
        {
            return;
        }

        /* UI previews and the picking pass are not the world replay, and a camera that has not
         * been positioned this frame is no basis for a distance ranking. */
        if (context.ui || context.isPicking() || context.camera.position.lengthSquared() == 0)
        {
            return;
        }

        /* A form pinned to the camera is the shot's own rig — it is always in frame, and
         * measuring its distance only ever wins it a slot it did not need. */
        if (form.anchor.get().hasTarget())
        {
            return;
        }

        double dx = context.entity.getX() - context.camera.position.x;
        double dy = context.entity.getY() - context.camera.position.y;
        double dz = context.entity.getZ() - context.camera.position.z;
        double distance = dx * dx + dy * dy + dz * dz;

        if (distance > budget && touched.add(form))
        {
            form.visible.setRuntimeValue(Boolean.FALSE);
        }
        else if (distance <= budget && touched.remove(form))
        {
            form.visible.setRuntimeValue(null);
        }
    }

    /**
     * Ranks this controller's forms by distance to the camera the world was just drawn through,
     * and keeps the closest {@link LodSettings#renderLimit} of them.
     */
    private static void onRenderAfter(BaseFilmController controller, WorldRenderContext context)
    {
        clearOverrides(controller);

        int limit = LodSettings.renderLimit.get();

        budgetValid = LodSettings.enabled.get() && limit > 0;

        if (!budgetValid)
        {
            return;
        }

        candidates.clear();
        Vec3d camera = context.camera().getPos();

        for (IEntity entity : controller.getEntities().values())
        {
            Form form = entity.getForm();

            if (form == null || form.anchor.get().hasTarget())
            {
                continue;
            }

            double dx = entity.getX() - camera.x;
            double dy = entity.getY() - camera.y;
            double dz = entity.getZ() - camera.z;

            candidates.add(new Candidate(form, dx * dx + dy * dy + dz * dz));
        }

        candidates.sort(null);

        /* The budget is the distance of the first form past the limit: everyone at or inside it
         * renders, everyone beyond it waits. Ties at the boundary keep both sides stable —
         * strict inequality in {@link #before} means a form exactly at the budget stays visible,
         * so the cap holds at its setting instead of flickering by one. */
        budget = candidates.size() > limit ? candidates.get(limit).distance : Double.POSITIVE_INFINITY;
    }

    private static void onShutdown(BaseFilmController controller)
    {
        clearOverrides(controller);

        budgetValid = false;
    }

    /**
     * Clears every runtime override the controller's root forms carry.
     *
     * <p>The shadow and name tag checks in {@code FilmEntityRenderer} run after
     * {@code FormUtilsClient.render} within the same entity render, so they see {@code visible ==
     * false} for a capped form too; and nothing leaks into the next frame's track application.</p>
     */
    private static void clearOverrides(BaseFilmController controller)
    {
        for (IEntity entity : controller.getEntities().values())
        {
            Form form = entity.getForm();

            if (form != null)
            {
                form.visible.setRuntimeValue(null);
            }
        }

        touched.clear();
    }

    private static class Candidate implements Comparable<Candidate>
    {
        public final Form form;
        public final double distance;

        public Candidate(Form form, double distance)
        {
            this.form = form;
            this.distance = distance;
        }

        @Override
        public int compareTo(Candidate other)
        {
            return Double.compare(this.distance, other.distance);
        }
    }
}
