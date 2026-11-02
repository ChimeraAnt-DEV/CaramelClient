package org.chimeramc.client.core.minecraft;

import org.chimeramc.client.util.ThermalPolicy;

/**
 * Suggests a gentler performance preset while the device is thermally throttled (item 260).
 *
 * <p>A throttled device already has less CPU to give, so chasing the fastest refresh mode and the
 * lowest input latency at that moment works against the user: the extra work is exactly what the
 * thermal governor is trying to shed, and the frame time gets worse rather than better. The
 * launcher cannot cool the device, so the honest deliverable is a suggestion — the user keeps the
 * final say, because the reading can be stale and some players prefer frames over heat.
 *
 * <p>Pure: the severity is passed in, so the rule is unit-tested without a {@code PowerManager}.
 */
public final class ThermalPresetAdvisor {

    /** What, if anything, the UI should say about the current preset given the thermal reading. */
    public enum Advice {
        /** Nothing to say: the device is cool, or the preset already matches the reading. */
        NONE,
        /** The device is warm and a lighter preset would help; suggest it. */
        SUGGEST_LIGHTER
    }

    private ThermalPresetAdvisor() {
    }

    /**
     * Decides whether to suggest a lighter preset.
     *
     * <p>Only a device at {@link ThermalPolicy#SEVERITY_HOT} is worth interrupting for: a warm
     * device is normal under load, and nudging the user every time the reading leaves NONE would
     * make the advice noise. A preset already at Battery is never asked to go lighter — there is
     * nothing below it, and a suggestion that cannot be acted on is just an error message.
     *
     * @param severity the current {@link ThermalPolicy} severity
     * @param currentPresetName the stored preset's {@code name()} ({@code BATTERY}/{@code BALANCED}/
     *                          {@code PERFORMANCE}); unknown values are treated as Balanced
     */
    public static Advice advise(int severity, String currentPresetName) {
        if (severity < ThermalPolicy.SEVERITY_HOT) {
            return Advice.NONE;
        }
        if ("BATTERY".equals(currentPresetName)) {
            return Advice.NONE;
        }
        return Advice.SUGGEST_LIGHTER;
    }

    /**
     * The preset to move to when the user accepts, or {@code null} when there is nothing to accept.
     *
     * <p>Performance drops straight to Battery (the throttle is the strongest signal there is),
     * while Balanced drops to Battery too rather than to a middle step that does not exist.
     */
    public static String suggestedPresetName(String currentPresetName) {
        if ("BATTERY".equals(currentPresetName)) {
            return null;
        }
        return "BATTERY";
    }
}
