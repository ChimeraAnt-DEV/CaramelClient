package org.chimeramc.client.ui.compose.launch;

import java.util.ArrayList;
import java.util.List;

/**
 * The Launch tab's quick-toggle row, as data.
 *
 * <p>Three glass cards — AntEgg packs, Voice, Replays — each with a live status line. The cards and
 * their statuses are one immutable list so the Compose row is a pure function of it, and the rule
 * that decides which cards are visible (guest mode hides Mods and Voice) is testable without a
 * device.
 */
public final class LaunchToggleState {

    /** Which quick toggle a card represents. */
    public enum Kind {
        ANTEGG,
        VOICE,
        REPLAY,
    }

    /** One card. */
    public static final class Toggle {
        public final Kind kind;
        public final String label;
        public final String status;

        public Toggle(Kind kind, String label, String status) {
            this.kind = kind;
            this.label = label;
            this.status = status;
        }
    }

    private LaunchToggleState() {
    }

    /**
     * The visible toggles, in order.
     *
     * <p>Guest mode hides AntEgg and Voice (the Mods and Voice surfaces); Replays stays, because it
     * is a local capture rather than a social or content surface. The caller supplies the label and
     * status strings, so this stays free of a Context.
     */
    public static List<Toggle> visible(boolean guestMode, String antEggLabel, String antEggStatus,
                                       String voiceLabel, String voiceStatus, String replayLabel,
                                       String replayStatus) {
        List<Toggle> out = new ArrayList<>();
        if (!guestMode) {
            out.add(new Toggle(Kind.ANTEGG, antEggLabel, antEggStatus));
            out.add(new Toggle(Kind.VOICE, voiceLabel, voiceStatus));
        }
        out.add(new Toggle(Kind.REPLAY, replayLabel, replayStatus));
        return out;
    }
}
