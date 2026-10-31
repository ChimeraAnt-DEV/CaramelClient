package org.chimeramc.client.core.antegg;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Remembers which {@code .AntEgg} packages are disabled (item 226).
 *
 * <p>The set is stored as the <em>disabled</em> ids rather than the enabled ones, so a package
 * installed after this preference was first written is enabled by default. Storing the enabled set
 * would mean every new package started disabled until the user noticed, which is the wrong default
 * for something the user deliberately installed.
 *
 * <p>The store never throws: an unreadable preference reads as "nothing disabled", because a
 * corrupt list must not make every mod appear off.
 */
public final class ScriptStateStore {

    private static final String PREFS_NAME = "antegg_script_state";
    private static final String KEY_DISABLED = "disabled_ids";

    private final SharedPreferences prefs;

    public ScriptStateStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The disabled package ids; never null. */
    public Set<String> disabledIds() {
        try {
            Set<String> stored = prefs.getStringSet(KEY_DISABLED, null);
            if (stored == null) return new LinkedHashSet<>();
            return new LinkedHashSet<>(stored);
        } catch (RuntimeException e) {
            return new LinkedHashSet<>();
        }
    }

    /** True when the package with this id is enabled (the default for an unknown id). */
    public boolean isEnabled(String id) {
        if (id == null || id.isEmpty()) return true;
        return !disabledIds().contains(id);
    }

    /** Enables or disables a package by id; a blank id is ignored. */
    public void setEnabled(String id, boolean enabled) {
        if (id == null || id.isEmpty()) return;
        Set<String> disabled = disabledIds();
        if (!applyEnabled(disabled, id, enabled)) return;
        prefs.edit().putStringSet(KEY_DISABLED, disabled).apply();
    }

    /**
     * Mutates a disabled-id set to reflect an enable/disable, returning true when it changed.
     *
     * <p>Pure, so the rule that a re-enable removes the id and a disable adds it is unit-tested
     * without a Context; the store method above is only the persistence around it.
     */
    static boolean applyEnabled(Set<String> disabled, String id, boolean enabled) {
        if (disabled == null || id == null || id.isEmpty()) return false;
        return enabled ? disabled.remove(id) : disabled.add(id);
    }

    /** The disabled ids as an unmodifiable snapshot, for a caller that iterates. */
    public Set<String> snapshot() {
        return Collections.unmodifiableSet(disabledIds());
    }
}
