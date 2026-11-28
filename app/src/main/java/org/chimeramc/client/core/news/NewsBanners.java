package org.chimeramc.client.core.news;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import org.chimeramc.client.R;

import java.util.Locale;

/**
 * The banner a news post draws above its text.
 *
 * <p>A published post cannot name a drawable: the document is fetched, and a resource id from a
 * remote file would be an instruction rather than data. So a post names one of a small fixed set of
 * keys and this maps them, and anything unrecognised falls back to the art its severity implies.
 * That way a post with no banner field still gets the right picture, and a typo in the field shows
 * the severity art rather than an empty hole. A row that has neither a key nor a severity, such as a
 * changelog section, gets no banner and keeps the card's gradient.
 */
public final class NewsBanners {

    /** The keys a post may name in its {@code banner} field. */
    public static final String KEY_EMERGENCY = "emergency";
    public static final String KEY_URGENT = "urgent";
    public static final String KEY_CHRISTMAS = "christmas";
    public static final String KEY_CARAMEL_PLUS = "caramel_plus";
    public static final String KEY_BERRYAPI = "berryapi";
    /** The card for the build that is in the store right now. */
    public static final String KEY_BETA10 = "beta10";

    /** No banner: the card draws its gradient. */
    public static final int NO_BANNER = 0;

    private NewsBanners() {
    }

    /**
     * The drawable for a post: an explicit key when it names a known banner, otherwise the one the
     * severity implies.
     */
    @DrawableRes
    public static int drawableFor(@Nullable String key, @Nullable NewsSeverity severity) {
        String normalised = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        switch (normalised) {
            case KEY_EMERGENCY:
                return R.drawable.news_banner_emergency;
            case KEY_URGENT:
                return R.drawable.news_banner_urgent;
            case KEY_CHRISTMAS:
                return R.drawable.news_banner_christmas;
            case KEY_CARAMEL_PLUS:
            case "caramel+":
            case "plus":
                return R.drawable.news_banner_plus;
            case KEY_BERRYAPI:
                return R.drawable.news_banner_berryapi;
            case KEY_BETA10:
                return R.drawable.news_banner_beta10;
            default:
                return severityDefault(severity);
        }
    }

    /**
     * The severity's art, or {@link #NO_BANNER} when the post carries no severity at all: an
     * announcement channel post always has one, while a changelog section has none, and a release
     * card should not wear an outage's picture.
     */
    @DrawableRes
    private static int severityDefault(@Nullable NewsSeverity severity) {
        if (severity == null) return NO_BANNER;
        return severity.isEmergency()
                ? R.drawable.news_banner_emergency
                : R.drawable.news_banner_urgent;
    }
}
