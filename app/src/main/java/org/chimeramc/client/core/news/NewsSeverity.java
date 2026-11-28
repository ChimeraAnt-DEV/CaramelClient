package org.chimeramc.client.core.news;

import java.util.Locale;

/**
 * How loud an emergency-news post is.
 *
 * <p>The channel carries two things that must not be confused: a service announcement the client can
 * afford to show as a normal news item, and a warning that something is broken right now. The
 * severity is part of the published document, not a guess from the text, because the same sentence
 * can be either ("sign-in is degraded" today, "sign-in is fixed" tomorrow).
 *
 * <p>Unknown or missing severity reads as {@link #URGENT}: an announcement that reaches this channel
 * at all was published as news, and under-reporting an outage is worse than an extra badge.
 */
public enum NewsSeverity {
    EMERGENCY("emergency"),
    URGENT("urgent");

    private final String wireName;

    NewsSeverity(String wireName) {
        this.wireName = wireName;
    }

    /** The value the published JSON carries for this severity. */
    public String wireName() {
        return wireName;
    }

    /** The severity for a published value, defaulting to {@link #URGENT}. */
    public static NewsSeverity fromWire(String value) {
        if (value == null) return URGENT;
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        for (NewsSeverity severity : values()) {
            if (severity.wireName.equals(normalised)) return severity;
        }
        return URGENT;
    }

    /** Whether this post outranks an ordinary announcement in the launcher's banner. */
    public boolean isEmergency() {
        return this == EMERGENCY;
    }
}
