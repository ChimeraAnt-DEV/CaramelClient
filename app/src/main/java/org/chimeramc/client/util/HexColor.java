package org.chimeramc.client.util;

/**
 * A pure parser for the {@code #RGB}/{@code #RRGGBB}/{@code #AARRGGBB} colour literals a mod config
 * stores (item 350).
 *
 * <p>{@code android.graphics.Color.parseColor} does the same job on a device, but it is not on the
 * unit-test classpath and it throws on anything it does not recognise - which is why both config
 * editors wrapped it in an empty {@code catch} and silently fell back to white. A stored value is
 * user-editable and can be corrupted by a hand-edited config file, so the parse has to be a rule
 * with a defined outcome rather than an exception to swallow.
 *
 * <p>This class is Android-free on purpose: the validation, the shorthand expansion and the
 * malformed-input behaviour are all pinned by {@link org.chimeramc.client.util.HexColorTest} with
 * no device.
 */
public final class HexColor {

    /** Opaque white, the fallback both editors used. */
    public static final int WHITE = 0xFFFFFFFF;

    private HexColor() {
    }

    /**
     * Parses {@code value} as a colour, returning {@code fallback} for anything malformed.
     *
     * <p>Accepted: {@code #RGB} (each digit doubled), {@code #RRGGBB} (opaque) and
     * {@code #AARRGGBB}. A leading {@code #} is required; the hex digits are case-insensitive. A
     * {@code null}, empty, wrongly-sized or non-hex value returns {@code fallback} rather than
     * throwing, so a corrupt config cannot take a config screen down.
     */
    public static int parseOr(String value, int fallback) {
        if (value == null) return fallback;
        String text = value.trim();
        if (text.isEmpty() || text.charAt(0) != '#') return fallback;
        int digits = text.length() - 1;
        long parsed = 0L;
        for (int i = 1; i < text.length(); i++) {
            int nibble = hexDigit(text.charAt(i));
            if (nibble < 0) return fallback;
            parsed = (parsed << 4) | nibble;
        }
        switch (digits) {
            case 3:
                // #RGB -> #RRGGBB, each digit doubled, then opaque.
                int r = (int) ((parsed >> 8) & 0xF);
                int g = (int) ((parsed >> 4) & 0xF);
                int b = (int) (parsed & 0xF);
                return 0xFF000000 | (r << 20) | (r << 16) | (g << 12) | (g << 8) | (b << 4) | b;
            case 6:
                return 0xFF000000 | (int) parsed;
            case 8:
                return (int) parsed;
            default:
                return fallback;
        }
    }

    /** Parses {@code value}, or {@link #WHITE} when it is malformed. */
    public static int parseOrWhite(String value) {
        return parseOr(value, WHITE);
    }

    /**
     * Whether {@code value} is a colour this parser accepts.
     *
     * <p>Used by the config editor to flag a bad value before it is saved, rather than accepting it
     * and silently rendering white.
     */
    public static boolean isValid(String value) {
        if (value == null) return false;
        String text = value.trim();
        if (text.isEmpty() || text.charAt(0) != '#') return false;
        int digits = text.length() - 1;
        if (digits != 3 && digits != 6 && digits != 8) return false;
        for (int i = 1; i < text.length(); i++) {
            if (hexDigit(text.charAt(i)) < 0) return false;
        }
        return true;
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
