package org.chimeramc.client.launcher.settings;

import org.chimeramc.client.settings.FeatureSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The one list of every setting: its id, section, title and description resource, search keywords
 * and how to read and write it.
 *
 * <p>The Settings screen is a 2,400-line layout with each switch wired by hand, so adding a setting
 * means editing XML in three places and the search box cannot find it. This registry is the single
 * description of a setting: a screen can be built from it, search reads it, and reset-to-default
 * comes for free. It is additive — the existing screen keeps working while it is adopted — and pure,
 * so the ordering, the search and the default-value rules are unit-tested without a device.
 *
 * <p>A setting's value is a {@link Getter}/{@link Setter} pair over the {@link FeatureSettings}
 * object, so the registry carries no state of its own and cannot drift from the store.
 */
public final class SettingsRegistry {

    /** The sections the screen groups settings into, in display order. */
    public enum Section {
        BASIC,
        PERSONALIZE,
        UPDATES,
        ABOUT,
        MIGRATION,
    }

    /** How a setting is presented and edited. */
    public enum Kind {
        TOGGLE,
        /** A choice from a small fixed set; the options are the getter's own values. */
        CHOICE,
        /** An action that fires once and has no stored value. */
        ACTION,
    }

    /** Reads a setting's current value from the store. */
    public interface Getter {
        boolean get(FeatureSettings settings);
    }

    /** Writes a setting's value to the store. */
    public interface Setter {
        void set(FeatureSettings settings, boolean value);
    }

    /** One setting. */
    public static final class Setting {
        public final String id;
        public final Section section;
        public final Kind kind;
        public final int titleRes;
        public final int descriptionRes;
        /** Extra search terms beyond the title, so a setting is findable by what it does. */
        public final List<String> keywords;
        public final Getter getter;
        public final Setter setter;
        /** The value a reset returns it to. */
        public final boolean defaultValue;

        Setting(String id, Section section, Kind kind, int titleRes, int descriptionRes,
                List<String> keywords, Getter getter, Setter setter, boolean defaultValue) {
            this.id = id;
            this.section = section;
            this.kind = kind;
            this.titleRes = titleRes;
            this.descriptionRes = descriptionRes;
            this.keywords = Collections.unmodifiableList(keywords);
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        /** The current value, or the default for an action (which has no stored value). */
        public boolean value(FeatureSettings settings) {
            return kind == Kind.ACTION ? false : getter.get(settings);
        }

        /** Applies a value; a no-op for an action, which is fired by its own handler. */
        public void apply(FeatureSettings settings, boolean value) {
            if (kind == Kind.ACTION || setter == null) return;
            setter.set(settings, value);
        }

        /** Whether the setting is at its default. An action is always "at default". */
        public boolean isDefault(FeatureSettings settings) {
            return kind == Kind.ACTION || value(settings) == defaultValue;
        }
    }

    private static final List<Setting> SETTINGS = build();

    private SettingsRegistry() {
    }

    /** Every setting, in registration order (which is the display order within a section). */
    public static List<Setting> all() {
        return SETTINGS;
    }

    /** The settings in one section, in order. */
    public static List<Setting> inSection(Section section) {
        List<Setting> out = new ArrayList<>();
        for (Setting s : SETTINGS) {
            if (s.section == section) out.add(s);
        }
        return out;
    }

    /** The setting with this id, or null. */
    public static Setting find(String id) {
        if (id == null) return null;
        for (Setting s : SETTINGS) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    /**
     * The settings matching a search query, in registration order.
     *
     * <p>Matches on the id, the keywords and the caller-supplied title/description text, so the
     * registry stays resource-free and the caller resolves the strings. An empty query matches
     * everything.
     */
    public static List<Setting> search(String query, TextLookup text) {
        List<Setting> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Setting s : SETTINGS) {
            if (q.isEmpty() || matches(s, q, text)) out.add(s);
        }
        return out;
    }

    /** The text for a setting's title and description, resolved by the caller. */
    public interface TextLookup {
        String title(Setting setting);

        String description(Setting setting);
    }

    private static boolean matches(Setting s, String q, TextLookup text) {
        if (s.id.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String k : s.keywords) {
            if (k.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        if (text != null) {
            String title = text.title(s);
            if (title != null && title.toLowerCase(Locale.ROOT).contains(q)) return true;
            String desc = text.description(s);
            if (desc != null && desc.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    /**
     * The ids whose value differs from the default, in order. A caller offers "reset changed
     * settings" from this.
     */
    public static List<String> nonDefaultIds(FeatureSettings settings) {
        List<String> out = new ArrayList<>();
        for (Setting s : SETTINGS) {
            if (!s.isDefault(settings)) out.add(s.id);
        }
        return out;
    }

    /** Resets every non-default setting to its default. Returns how many changed. */
    public static int resetToDefaults(FeatureSettings settings) {
        int changed = 0;
        for (Setting s : SETTINGS) {
            if (s.kind == Kind.ACTION) continue;
            if (!s.isDefault(settings)) {
                s.apply(settings, s.defaultValue);
                changed++;
            }
        }
        return changed;
    }

    private static List<Setting> build() {
        List<Setting> out = new ArrayList<>();
        Map<String, Setting> byId = new LinkedHashMap<>();

        // The settings that already exist and have a documented default. Each entry mirrors the
        // field's own declaration, so the registry cannot claim a default the store does not use.
        add(out, byId, new Setting("low_input_delay",
                Section.BASIC, Kind.TOGGLE,
                org.chimeramc.client.R.string.low_input_delay_title,
                org.chimeramc.client.R.string.low_input_delay_desc,
                list("latency", "input", "delay"),
                FeatureSettings::isLowInputDelayEnabled,
                FeatureSettings::setLowInputDelayEnabled, false));
        add(out, byId, new Setting("fps_optimizer",
                Section.BASIC, Kind.TOGGLE,
                org.chimeramc.client.R.string.fps_optimizer_title,
                org.chimeramc.client.R.string.fps_optimizer_desc,
                list("fps", "performance", "frames"),
                FeatureSettings::isFpsOptimizerEnabled,
                FeatureSettings::setFpsOptimizerEnabled, false));
        add(out, byId, new Setting("reduce_network_latency",
                Section.BASIC, Kind.TOGGLE,
                org.chimeramc.client.R.string.reduce_network_latency_title,
                org.chimeramc.client.R.string.reduce_network_latency_desc,
                list("network", "latency", "ping"),
                FeatureSettings::isReduceNetworkLatencyEnabled,
                FeatureSettings::setReduceNetworkLatencyEnabled, false));
        add(out, byId, new Setting("show_launch_log",
                Section.BASIC, Kind.TOGGLE,
                org.chimeramc.client.R.string.show_launch_log,
                org.chimeramc.client.R.string.show_launch_log_desc,
                list("launch", "log", "debug"),
                FeatureSettings::isLaunchLogEnabled,
                FeatureSettings::setLaunchLogEnabled, false));
        add(out, byId, new Setting("logcat_overlay",
                Section.BASIC, Kind.TOGGLE,
                org.chimeramc.client.R.string.show_logcat_overlay,
                org.chimeramc.client.R.string.show_logcat_overlay,
                list("logcat", "log", "debug"),
                FeatureSettings::isLogcatOverlayEnabled,
                FeatureSettings::setLogcatOverlayEnabled, false));

        add(out, byId, new Setting("guest_mode",
                Section.PERSONALIZE, Kind.TOGGLE,
                org.chimeramc.client.R.string.guest_mode_title,
                org.chimeramc.client.R.string.guest_mode_desc,
                list("guest", "privacy", "shared"),
                FeatureSettings::isGuestModeEnabled,
                FeatureSettings::setGuestModeEnabled, false));
        add(out, byId, new Setting("streaming_safe",
                Section.PERSONALIZE, Kind.TOGGLE,
                org.chimeramc.client.R.string.streaming_safe_title,
                org.chimeramc.client.R.string.streaming_safe_desc,
                list("stream", "privacy", "mask"),
                FeatureSettings::isStreamingSafeModeEnabled,
                FeatureSettings::setStreamingSafeModeEnabled, false));

        add(out, byId, new Setting("mod_update_check",
                Section.UPDATES, Kind.TOGGLE,
                org.chimeramc.client.R.string.mod_update_check_title,
                org.chimeramc.client.R.string.mod_update_check_desc,
                list("update", "mods", "check"),
                FeatureSettings::isModUpdateCheckEnabled,
                FeatureSettings::setModUpdateCheckEnabled, true));

        add(out, byId, new Setting("crash_upload",
                Section.ABOUT, Kind.TOGGLE,
                org.chimeramc.client.R.string.crash_upload,
                org.chimeramc.client.R.string.crash_upload_preview,
                list("crash", "report", "privacy"),
                FeatureSettings::isCrashUploadEnabled,
                FeatureSettings::setCrashUploadEnabled, false));
        add(out, byId, new Setting("usage_stats",
                Section.ABOUT, Kind.TOGGLE,
                org.chimeramc.client.R.string.usage_stats_title,
                org.chimeramc.client.R.string.usage_stats_desc,
                list("usage", "telemetry", "privacy"),
                FeatureSettings::isUsageStatsEnabled,
                FeatureSettings::setUsageStatsEnabled, false));

        return out;
    }

    private static void add(List<Setting> out, Map<String, Setting> byId, Setting setting) {
        Setting existing = byId.put(setting.id, setting);
        if (existing != null) {
            throw new IllegalStateException("duplicate setting id: " + setting.id);
        }
        out.add(setting);
    }

    private static List<String> list(String... items) {
        List<String> out = new ArrayList<>();
        Collections.addAll(out, items);
        return out;
    }
}
