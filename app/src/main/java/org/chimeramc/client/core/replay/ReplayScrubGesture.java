package org.chimeramc.client.core.replay;

/**
 * The hold-to-scrub drag rules, as pure maths.
 *
 * <p>{@link ClipCardView} used to decide inline whether a drag had passed touch slop and had turned
 * horizontal, and it divided the raw x by the view width to get the position. That is the kind of
 * arithmetic where an off-by-one (dividing by the padding box, or starting a vertical scroll as a
 * scrub) is invisible in review and only shows as a card that previews when the list is scrolled.
 *
 * <p>This holds the decision and the fraction. It is Android-free, so a JVM test pins every case.
 */
public final class ReplayScrubGesture {

    /** Whether a drag that started at {@code down} and is now at {@code x,y} begins a scrub. */
    public static boolean isScrub(float downX, float downY, float x, float y, int touchSlop) {
        float dx = Math.abs(x - downX);
        float dy = Math.abs(y - downY);
        // Horizontal past slop, and more horizontal than vertical: a mostly-vertical drag is the
        // list scrolling, and must not be read as a scrub.
        return dx > touchSlop && dx > dy;
    }

    /** The position through the clip for a touch x, clamped to 0..1. */
    public static float fraction(float x, int width) {
        if (width <= 0) return 0f;
        return Math.max(0f, Math.min(1f, x / (float) width));
    }

    private ReplayScrubGesture() {
    }
}
