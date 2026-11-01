package org.chimeramc.client.launcher.ui.util;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.app.ActivityOptionsCompat;

import org.chimeramc.client.util.PersonalizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the scene-transition options for a list-to-detail navigation (item 123).
 *
 * <p>The detail screens used a plain {@code startActivity}, so the icon and title jumped between
 * screens. A shared-element transition carries them across instead, which is the difference between
 * "a new screen appeared" and "this item opened". {@link ActivityOptionsCompat} keeps this on API
 * 28 without a version branch.
 *
 * <p>The transition is skipped under reduced motion. A shared element animates the two screens
 * together, so it is exactly the kind of motion the accessibility switch exists to remove, and a
 * user who asked for no animation should get an instant swap rather than a shorter one.
 */
public final class SceneTransitions {

    /** Shared-element names, declared in both the list item and the detail layout. */
    public static final String NAME_ICON = "chimera_transition_icon";
    public static final String NAME_TITLE = "chimera_transition_title";

    private SceneTransitions() {
    }

    /** A view and the transition name it is shared under. */
    public static final class SharedElement {
        final View view;
        final String name;

        public SharedElement(View view, String name) {
            this.view = view;
            this.name = name;
        }
    }

    /** Pairs a view with the icon transition name when the view is present. */
    public static SharedElement icon(View view) {
        return view == null ? null : new SharedElement(view, NAME_ICON);
    }

    /** Pairs a view with the title transition name when the view is present. */
    public static SharedElement title(View view) {
        return view == null ? null : new SharedElement(view, NAME_TITLE);
    }

    /**
     * Starts {@code intent} from {@code activity} with a shared-element transition built from
     * {@code elements}.
     *
     * <p>Falls back to a plain start when there is nothing to share or the user prefers reduced
     * motion, so a caller never has to branch on either.
     */
    public static void start(Activity activity, Intent intent, SharedElement... elements) {
        if (activity == null || intent == null) return;
        if (new PersonalizationManager(activity).isReduceMotion()) {
            activity.startActivity(intent);
            return;
        }
        List<SharedElement> usable = new ArrayList<>();
        if (elements != null) {
            for (SharedElement element : elements) {
                if (element != null && element.view != null && element.name != null) {
                    usable.add(element);
                }
            }
        }
        if (usable.isEmpty()) {
            activity.startActivity(intent);
            return;
        }
        ActivityOptionsCompat options = optionsFor(activity, usable);
        if (options == null) {
            activity.startActivity(intent);
            return;
        }
        try {
            activity.startActivity(intent, options.toBundle());
        } catch (RuntimeException e) {
            // A shared element whose name does not resolve throws; the navigation itself is not
            // worth failing for, so fall back to the plain start.
            activity.startActivity(intent);
        }
    }

    private static ActivityOptionsCompat optionsFor(Activity activity, List<SharedElement> usable) {
        try {
            if (usable.size() == 1) {
                SharedElement only = usable.get(0);
                return ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity, only.view, only.name);
            }
            @SuppressWarnings("unchecked")
            androidx.core.util.Pair<View, String>[] pairs = new androidx.core.util.Pair[usable.size()];
            for (int i = 0; i < usable.size(); i++) {
                pairs[i] = androidx.core.util.Pair.create(usable.get(i).view, usable.get(i).name);
            }
            return ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Applies the shared-element names to a detail screen's views. */
    public static void tag(View view, String name) {
        if (view != null && name != null) {
            view.setTransitionName(name);
        }
    }
}
