package org.chimeramc.client.ui.compose.launch;

import org.chimeramc.client.core.changelog.LaunchChangelog;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the parsed release notes onto the Compose state the card and popup render.
 *
 * <p>Kept as a separate, pure step rather than folded into the parser so the bucket mapping has one
 * home: the card counts the buckets and the popup lists them, and both must see the same grouping.
 */
public final class LaunchChangelogStateBuilder {

    private LaunchChangelogStateBuilder() {
    }

    /** Builds the state from the parsed changelog and the build's version name. */
    public static LaunchChangelogState build(LaunchChangelog changelog, String version) {
        if (changelog == null) {
            return new LaunchChangelogState(
                    version == null ? "" : version,
                    "", "", emptyList(), emptyList(), emptyList(), emptyList());
        }
        return new LaunchChangelogState(
                version == null ? "" : version,
                changelog.headline,
                changelog.summary,
                map(changelog.entriesOfKind(LaunchChangelog.Kind.ADDED)),
                map(changelog.entriesOfKind(LaunchChangelog.Kind.CHANGED)),
                map(changelog.entriesOfKind(LaunchChangelog.Kind.FIXED)),
                map(changelog.entriesOfKind(LaunchChangelog.Kind.OTHER)));
    }

    private static List<ChangelogEntry> map(List<LaunchChangelog.Entry> entries) {
        List<ChangelogEntry> out = new ArrayList<>();
        if (entries == null) return out;
        for (LaunchChangelog.Entry entry : entries) {
            if (entry == null) continue;
            out.add(new ChangelogEntry(entry.text, entry.url));
        }
        return out;
    }

    private static List<ChangelogEntry> emptyList() {
        return new ArrayList<>();
    }
}
