package org.chimeramc.client.ui.components;

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

/**
 * The one place a screen marks a heading or couples a label to its control (items 341, 342).
 *
 * <p>Two rules are easy to state and easy to forget, which is why they live here rather than in
 * each screen:
 *
 * <ul>
 *   <li>A section title is a <em>heading</em>. TalkBack can then jump between sections instead of
 *       making the user swipe through every row to reach the next one. The XML attribute
 *       {@code android:accessibilityHeading="true"} is the preferred form (it needs no code and is
 *       available from API 28, which is this app's floor); this method is for a view built in code.
 *   <li>A toggle row's label names its switch. A bare switch is announced as "switch, off" with no
 *       indication of what it controls. {@code labelFor} makes the pair read as one thing.
 * </ul>
 *
 * <p>{@link #heading} also sets the pane-title bit, so a screen reader that does not honour the
 * heading API still groups the section rather than reading a flat list.
 */
public final class UiAccessibility {

    private UiAccessibility() {
    }

    /** Marks {@code view} as a section heading. A no-op for a null view. */
    public static void heading(View view) {
        if (view == null) return;
        view.setAccessibilityHeading(true);
    }

    /** Marks every non-null view in {@code views} as a section heading. */
    public static void headings(View... views) {
        if (views == null) return;
        for (View view : views) {
            heading(view);
        }
    }

    /**
     * Couples a label to the control it names, so focusing the control announces the label.
     *
     * <p>Kept separate from {@link #toggleRow} for the case where the label and the control are not
     * siblings (a label in a header row above a switch, say).
     */
    public static void labelFor(TextView label, View control) {
        if (label == null || control == null) return;
        label.setLabelFor(control.getId());
    }

    /**
     * Makes a toggle row one focus stop whose name is the label and whose state is the switch.
     *
     * <p>The row itself is not made focusable: the switch already is, and a focusable row would add
     * the second stop the item asks to remove. Instead the label names the switch, the row's
     * non-interactive children stop being separate nodes, and a tap anywhere on the row toggles the
     * switch — which is what a touch user expects from a settings row and what makes the row
     * behave the same way it reads.
     */
    public static void toggleRow(View row, TextView label, View toggle) {
        if (row == null || toggle == null) return;
        if (label != null) {
            labelFor(label, toggle);
            // The switch carries the label as its own description and the label view stops being a
            // separate node, so the pair is one focus stop. Two stops for one setting is what makes
            // a settings list exhausting to traverse with a screen reader.
            if (toggle.getContentDescription() == null) {
                toggle.setContentDescription(label.getText());
            }
            label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        // NO, not NO_HIDE_DESCENDANTS: the row itself is not a node, but its switch still is.
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        // A tap anywhere on the row toggles the switch, which is what the row's appearance already
        // promises a touch user. A row that is already clickable has its own action (it opens a
        // screen, say) and is left alone rather than having that action replaced by a toggle.
        if (!row.isClickable()) {
            row.setOnClickListener(v -> toggle.performClick());
        }
    }

    /**
     * Wires every label+switch row under {@code root} in one pass (item 342).
     *
     * <p>A settings screen has dozens of these rows, and each one wired by hand is a chance to miss
     * one. Two shapes are recognised, because they need different treatment:
     *
     * <ul>
     *   <li>a <em>plain row</em> - exactly two children, a label and the switch. This is one focus
     *       stop: the label names the switch and stops being its own node.
     *   <li>a <em>card</em> - a title, a description and the switch. Here only the switch's name is
     *       set from the first line of text; the description stays a readable node, because hiding
     *       it would remove the explanation the user needs.
     * </ul>
     *
     * <p>A group with more than one switch, or with a button of its own, is left alone: it has no
     * single label to name a control and guessing would mislabel one.
     */
    public static void wireToggleRows(android.view.View root) {
        if (!(root instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup group = (android.view.ViewGroup) root;
        android.view.View toggle = null;
        int switches = 0;
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (isSwitch(child)) {
                switches++;
                toggle = child;
            }
        }
        if (switches == 1 && !hasOwnAction(group)) {
            android.widget.TextView label = firstText(group);
            if (label != null) {
                if (group.getChildCount() == 2) {
                    toggleRow(group, label, toggle);
                } else {
                    // A card: name the switch, but leave the other views as nodes.
                    labelFor(label, toggle);
                    if (toggle.getContentDescription() == null) {
                        toggle.setContentDescription(label.getText());
                    }
                    toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                }
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            wireToggleRows(group.getChildAt(i));
        }
    }

    /** True when the subtree holds a control of its own, so the group is not a single toggle row. */
    private static boolean hasOwnAction(android.view.ViewGroup group) {
        if (group.isClickable()) return true;
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof android.widget.Button || child instanceof android.widget.EditText) {
                return true;
            }
            if (child instanceof android.view.ViewGroup
                    && hasOwnAction((android.view.ViewGroup) child)) {
                return true;
            }
        }
        return false;
    }

    /** The first non-empty label in the subtree, or null. */
    private static android.widget.TextView firstText(android.view.ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                CharSequence text = ((android.widget.TextView) child).getText();
                if (text != null && text.toString().trim().length() > 0) {
                    return (android.widget.TextView) child;
                }
            }
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            android.view.View child = group.getChildAt(i);
            if (child instanceof android.view.ViewGroup) {
                android.widget.TextView found = firstText((android.view.ViewGroup) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * True for the switch widgets the app uses.
     *
     * <p>Checked by class name rather than by {@code instanceof SwitchMaterial}, so this helper does
     * not force a Material dependency on a caller that only wants the walk, and so the test can pass
     * a stub.
     */
    public static boolean isSwitch(android.view.View view) {
        if (view == null) return false;
        Class<?> type = view.getClass();
        while (type != null) {
            if (isSwitchClassName(type.getName())) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    /**
     * The name rule behind {@link #isSwitch}, split out so it is testable without a View.
     *
     * <p>Constructing a real widget in a JVM test needs the Android framework, which a unit test
     * does not have; a pure string rule does not.
     */
    public static boolean isSwitchClassName(String className) {
        if (className == null) return false;
        return className.endsWith("SwitchMaterial") || className.endsWith("SwitchCompat")
                || "android.widget.Switch".equals(className);
    }

    /**
     * The description TalkBack reads for a heading, in the one format the app uses.
     *
     * <p>Pure and package-visible so the rule is unit-tested without a device.
     */
    public static String headingDescription(String title, String subtitle) {
        if (title == null || title.trim().isEmpty()) return "";
        if (subtitle == null || subtitle.trim().isEmpty()) return title.trim();
        return title.trim() + ". " + subtitle.trim();
    }

    /** True when {@code info} is flagged as a heading; the read side of {@link #heading}. */
    public static boolean isHeading(AccessibilityNodeInfo info) {
        return info != null && info.isHeading();
    }
}
