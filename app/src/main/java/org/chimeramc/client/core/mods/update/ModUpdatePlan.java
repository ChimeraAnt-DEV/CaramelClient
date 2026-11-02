package org.chimeramc.client.core.mods.update;

import org.chimeramc.client.launcher.util.UpdateVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which installed mods have a newer version published (item 273).
 *
 * <p>The point of this class is that the launcher makes <em>one</em> statement about updates rather
 * than interrupting per mod. A mod hub that prompts for each mod turns a library of twenty mods into
 * twenty interruptions; a single summary ("3 mods have updates") lets the user decide when to look.
 * The rule is therefore pure and Android-free, so the "what counts as an update" decision is
 * unit-tested instead of being re-derived at each call site.
 *
 * <p>Version comparison reuses {@link UpdateVersion}, the same rule the launcher's own update check
 * uses, so a mod tag like {@code BETA5} orders the same way there and here.
 */
public final class ModUpdatePlan {

    private ModUpdatePlan() {
    }

    /** One installed mod and the version the feed publishes for it. */
    public static final class ModUpdate {
        public final String id;
        public final String name;
        public final String installedVersion;
        public final String availableVersion;

        public ModUpdate(String id, String name, String installedVersion, String availableVersion) {
            this.id = id == null ? "" : id;
            this.name = name == null || name.isEmpty() ? this.id : name;
            this.installedVersion = installedVersion == null ? "" : installedVersion;
            this.availableVersion = availableVersion == null ? "" : availableVersion;
        }
    }

    /**
     * The updates for the installed mods, in the order the mods were given.
     *
     * <p>A mod with no feed entry is skipped, an entry that is not newer is skipped, and an entry
     * whose id does not correspond to an installed mod is ignored — the feed describes what exists,
     * and an id nobody has installed is not the user's problem. A mod whose installed version is
     * blank still counts as updatable, because "unknown version" is not "up to date".
     */
    public static List<ModUpdate> compute(List<InstalledMod> installed,
                                          Map<String, String> feed) {
        List<ModUpdate> updates = new ArrayList<>();
        if (installed == null || feed == null || feed.isEmpty()) return updates;

        // Normalise the feed's keys once so a case difference between the feed and the manifest
        // does not hide an update.
        Map<String, String> normalised = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : feed.entrySet()) {
            if (entry.getKey() == null) continue;
            String value = entry.getValue();
            if (value == null || value.trim().isEmpty()) continue;
            normalised.put(entry.getKey().trim().toLowerCase(java.util.Locale.ROOT), value.trim());
        }

        for (InstalledMod mod : installed) {
            if (mod == null || mod.id == null || mod.id.trim().isEmpty()) continue;
            String available = normalised.get(mod.id.trim().toLowerCase(java.util.Locale.ROOT));
            if (available == null) continue;
            String current = mod.version == null ? "" : mod.version.trim();
            if (current.isEmpty() || UpdateVersion.isNewer(available, current)) {
                updates.add(new ModUpdate(mod.id, mod.name, current, available));
            }
        }
        return updates;
    }

    /**
     * A stable fingerprint of a set of updates, used to decide whether anything changed since the
     * last announcement (item 273).
     *
     * <p>Sorted so feed order cannot produce a different fingerprint for the same updates, and
     * built from {@code id@version} so a mod that gains a newer version changes it. An empty list
     * has an empty fingerprint.
     */
    public static java.util.Set<String> fingerprint(List<ModUpdate> updates) {
        java.util.Set<String> result = new java.util.TreeSet<>();
        if (updates == null) return result;
        for (ModUpdate update : updates) {
            if (update == null) continue;
            result.add(update.id + "@" + update.availableVersion);
        }
        return result;
    }

    /** The installed-mod view this rule needs, so it stays free of the launcher's Mod type. */
    public static final class InstalledMod {
        public final String id;
        public final String name;
        public final String version;

        public InstalledMod(String id, String name, String version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }
    }
}
