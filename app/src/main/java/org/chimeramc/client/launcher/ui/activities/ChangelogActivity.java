package org.chimeramc.client.launcher.ui.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ActivityChangelogBinding;
import org.chimeramc.client.launcher.core.changelog.ChangelogMarkdown;
import org.chimeramc.client.core.changelog.ChangelogFeed;
import org.chimeramc.client.ui.activities.BaseActivity;
import org.chimeramc.client.util.AppLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The in-app changelog screen, rendered from the repo's {@code CHANGELOG.md}.
 *
 * <p>One source of truth: the file is copied into the assets at build time and parsed by the pure
 * {@link ChangelogMarkdown}, so the screen cannot describe a release differently from the file a
 * contributor edits. The parse and the file read run off the UI thread, because the file is not
 * needed for the first frame.
 */
public class ChangelogActivity extends BaseActivity {

    private static final String TAG = "ChangelogActivity";
    private static final String ASSET_PATH = "launcher/CHANGELOG.md";
    /** The two-section feed: what shipped, what is next, and the sneak peeks. */
    private static final String FEED_ASSET_PATH = "launcher/changelog_feed.json";
    /** Where the feed's sneak-peek images are packaged. */
    private static final String SNEAK_ASSET_DIR = "launcher/sneak/";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityChangelogBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangelogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setActiveNavTab(R.id.nav_tab_settings);

        binding.changelogScreenVersion.setText(
                getString(R.string.changelog_version, BuildConfig.VERSION_NAME));

        loadChangelog(binding.changelogScreenContent);
    }

    private void loadChangelog(LinearLayout content) {
        try {
            executor.execute(() -> {
                String markdown = readAsset();
                List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse(markdown);
                runOnUiThread(() -> render(content, blocks));
            });
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            AppLog.warn(TAG, "Changelog read dropped: the screen is gone", rejected);
        }
    }

    private String readAsset() {
        try (InputStream in = getAssets().open(ASSET_PATH);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Throwable t) {
            AppLog.warn(TAG, "Reading the bundled changelog failed", t);
            return null;
        }
    }

    private void render(LinearLayout parent, List<ChangelogMarkdown.Block> blocks) {
        parent.removeAllViews();
        // The two-section feed comes first: newest, then upcoming, then the sneak peeks. It is a
        // separate source from the markdown (the feed is curated for the app; the markdown is the
        // contributor-facing history), so both are shown rather than one replacing the other.
        renderFeed(parent, readFeed());
        if (!ChangelogMarkdown.hasContent(blocks)) {
            TextView empty = new TextView(this);
            empty.setText(R.string.changelog_empty);
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setTypeface(getResources().getFont(R.font.misans));
            parent.addView(empty);
            return;
        }
        for (ChangelogMarkdown.Block block : blocks) {
            parent.addView(renderBlock(block));
        }
    }

    /** Reads the feed asset; a missing or unreadable feed is empty, never an error. */
    private ChangelogFeed readFeed() {
        try (InputStream in = getAssets().open(FEED_ASSET_PATH)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return ChangelogFeed.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Throwable t) {
            return ChangelogFeed.empty();
        }
    }

    /**
     * Renders the two-section feed: the newest release, the upcoming work, then the sneak peeks.
     *
     * <p>An empty section is skipped rather than shown as an empty heading, so a feed with only the
     * newest release reads as one section, not two with a blank second.
     */
    private void renderFeed(LinearLayout parent, ChangelogFeed feed) {
        if (feed == null) return;
        if (feed.hasNewest()) {
            parent.addView(feedHeader(getString(R.string.changelog_section_newest),
                    feed.newest().version));
            if (!feed.newest().headline.isEmpty()) {
                parent.addView(feedHeadline(feed.newest().headline));
            }
            for (String item : feed.newest().items) parent.addView(feedBullet(item));
        }
        if (feed.hasUpcoming()) {
            parent.addView(feedHeader(getString(R.string.changelog_section_upcoming),
                    feed.upcoming().version));
            if (!feed.upcoming().headline.isEmpty()) {
                parent.addView(feedHeadline(feed.upcoming().headline));
            }
            for (String item : feed.upcoming().items) parent.addView(feedBullet(item));
        }
        if (!feed.sneakPeeks().isEmpty()) {
            parent.addView(feedHeader(getString(R.string.changelog_section_sneak_peeks), ""));
            for (ChangelogFeed.SneakPeek peek : feed.sneakPeeks()) {
                View tile = sneakPeekTile(peek);
                if (tile != null) parent.addView(tile);
            }
        }
    }

    private View feedHeader(String title, String version) {
        float density = getResources().getDisplayMetrics().density;
        TextView tv = new TextView(this);
        tv.setText(version == null || version.isEmpty() ? title : title + " · " + version);
        tv.setTextColor(getColor(R.color.primary));
        tv.setTypeface(getResources().getFont(R.font.misans), Typeface.BOLD);
        tv.setTextSize(18);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (20 * density);
        lp.bottomMargin = (int) (6 * density);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View feedHeadline(String headline) {
        TextView tv = new TextView(this);
        tv.setText(headline);
        tv.setTextColor(getColor(R.color.on_surface));
        tv.setTypeface(getResources().getFont(R.font.misans), Typeface.BOLD);
        tv.setTextSize(14);
        return tv;
    }

    private View feedBullet(String text) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, (int) (6 * density), 0, 0);
        TextView bullet = new TextView(this);
        bullet.setText("\u2022");
        bullet.setTextColor(getColor(R.color.primary));
        bullet.setTextSize(16);
        bullet.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (18 * density), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextColor(getColor(R.color.text_secondary));
        body.setTypeface(getResources().getFont(R.font.misans));
        body.setTextSize(13);
        body.setLineSpacing(0, 1.08f);
        body.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(bullet);
        row.addView(body);
        return row;
    }

    /**
     * One sneak peek: the image with its caption beneath.
     *
     * <p>The image is loaded by the file name the feed carries from the packaged sneak directory; a
     * missing image is skipped rather than shown as a broken tile.
     */
    private View sneakPeekTile(ChangelogFeed.SneakPeek peek) {
        android.graphics.Bitmap bitmap = null;
        try (InputStream in = getAssets().open(SNEAK_ASSET_DIR + peek.image)) {
            bitmap = android.graphics.BitmapFactory.decodeStream(in);
        } catch (Throwable t) {
            bitmap = null;
        }
        if (bitmap == null) return null;

        float density = getResources().getDisplayMetrics().density;
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (10 * density);
        column.setLayoutParams(lp);

        android.widget.ImageView image = new android.widget.ImageView(this);
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(peek.caption);
        column.addView(image);

        if (!peek.caption.isEmpty()) {
            TextView caption = new TextView(this);
            caption.setText(peek.caption);
            caption.setTextColor(getColor(R.color.text_secondary));
            caption.setTypeface(getResources().getFont(R.font.misans));
            caption.setTextSize(12);
            caption.setGravity(Gravity.CENTER);
            caption.setPadding(0, (int) (4 * density), 0, 0);
            column.addView(caption);
        }
        return column;
    }

    private View renderBlock(ChangelogMarkdown.Block block) {
        float density = getResources().getDisplayMetrics().density;
        switch (block.kind) {
            case VERSION: {
                TextView tv = new TextView(this);
                tv.setText(block.text);
                tv.setTextColor(getColor(R.color.primary));
                tv.setTypeface(getResources().getFont(R.font.misans), Typeface.BOLD);
                tv.setTextSize(18);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (20 * density);
                lp.bottomMargin = (int) (6 * density);
                tv.setLayoutParams(lp);
                return tv;
            }
            case SECTION: {
                TextView tv = new TextView(this);
                tv.setText(block.text);
                tv.setTextColor(getColor(R.color.on_surface));
                tv.setTypeface(getResources().getFont(R.font.misans), Typeface.BOLD);
                tv.setTextSize(14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (14 * density);
                tv.setLayoutParams(lp);
                return tv;
            }
            case BULLET: {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, (int) (6 * density), 0, 0);

                TextView bullet = new TextView(this);
                bullet.setText("•");
                bullet.setTextColor(getColor(R.color.primary));
                bullet.setTextSize(16);
                bullet.setLayoutParams(new LinearLayout.LayoutParams(
                        (int) (18 * density), LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView text = new TextView(this);
                text.setText(block.text);
                text.setTextColor(getColor(R.color.text_secondary));
                text.setTypeface(getResources().getFont(R.font.misans));
                text.setTextSize(13);
                text.setLineSpacing(0, 1.08f);
                text.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                row.addView(bullet);
                row.addView(text);
                return row;
            }
            case BODY:
            default: {
                TextView tv = new TextView(this);
                tv.setText(block.text);
                tv.setTextColor(getColor(R.color.text_secondary));
                tv.setTypeface(getResources().getFont(R.font.misans));
                tv.setTextSize(13);
                tv.setGravity(Gravity.START);
                tv.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                if (TextUtils.isEmpty(block.text)) {
                    tv.setVisibility(View.GONE);
                }
                return tv;
            }
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
