package org.chimeramc.client.core.changelog;

import android.content.Context;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.compose.caramel.CaramelNewsInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The announcements that ship with the client, as opposed to the ones read from the changelog feed.
 *
 * <p>Each one is a bundled string set rather than a fetched document, so the News tab has something
 * to say on a fresh install with no network. A post that advertises something not yet on sale says
 * so in its own words: the tab is read as a promise, and a post that oversells is a lie the client
 * has to keep.
 */
public final class BundledNews {

    private BundledNews() {
    }

    /** Every bundled post, newest first. */
    public static List<CaramelNewsInfo> posts(Context context) {
        List<CaramelNewsInfo> out = new ArrayList<>();
        out.add(post(context, R.string.caramel_news_offsets_title, R.string.caramel_news_offsets_body,
                R.string.caramel_news_offsets_meta, R.drawable.news_banner_offsets));
        out.add(post(context, R.string.caramel_news_christmas_shop_title,
                R.string.caramel_news_christmas_shop_body,
                R.string.caramel_news_christmas_shop_meta, R.drawable.news_banner_christmas_shop));
        out.add(post(context, R.string.caramel_news_plus_title, R.string.caramel_news_plus_body,
                R.string.caramel_news_plus_meta, R.drawable.news_banner_plus));
        out.add(post(context, R.string.caramel_news_christmas_title,
                R.string.caramel_news_christmas_body, R.string.caramel_news_christmas_meta,
                R.drawable.news_banner_christmas));
        out.add(new CaramelNewsInfo(
                context.getString(R.string.caramel_news_honey_title),
                context.getString(R.string.caramel_news_honey_body),
                context.getString(R.string.caramel_news_honey_meta),
                false,
                context.getString(R.string.caramel_news_honey_button),
                "https://honeyclient.github.io/",
                R.drawable.news_banner_honey));
        return out;
    }

    private static CaramelNewsInfo post(Context context, int title, int body, int meta, int banner) {
        return new CaramelNewsInfo(context.getString(title), context.getString(body),
                context.getString(meta), false, "", "", banner);
    }
}
