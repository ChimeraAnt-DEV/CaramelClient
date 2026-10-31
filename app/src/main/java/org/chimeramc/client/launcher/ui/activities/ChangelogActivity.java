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
import org.chimeramc.client.launcher.core.changelog.ChangelogMarkdown;
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
 * The in-app changelog screen (item 270), rendered from the repo's {@code CHANGELOG.md}.
 *
 * <p>One source of truth: the file is copied into the assets at build time and parsed by the pure
 * {@link ChangelogMarkdown}, so the screen cannot describe a release differently from the file a
 * contributor edits. The parse and the file read run off the UI thread, because the file is not
 * needed for the first frame.
 */
public class ChangelogActivity extends BaseActivity {

    private static final String TAG = "ChangelogActivity";
    private static final String ASSET_PATH = "launcher/CHANGELOG.md";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);
        setActiveNavTab(R.id.nav_tab_settings);

        TextView version = findViewById(R.id.changelog_screen_version);
        if (version != null) {
            version.setText(getString(R.string.changelog_version, BuildConfig.VERSION_NAME));
        }

        LinearLayout content = findViewById(R.id.changelog_screen_content);
        if (content == null) return;
        loadChangelog(content);
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
