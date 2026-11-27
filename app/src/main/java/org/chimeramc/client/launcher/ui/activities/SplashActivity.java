package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.R;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.launcher.core.splash.SplashArt;
import org.chimeramc.client.launcher.core.splash.SplashArtCatalog;
import org.chimeramc.client.launcher.core.splash.SplashShow;
import org.chimeramc.client.launcher.core.splash.SplashSound;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelSplashHost;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.MainThread;

import java.util.Random;

/**
 * The cold-start splash: a twenty-second key-art show, then the launcher.
 *
 * <p>The show owns the clock. The screen picks a handful of the bundled key art at random — never
 * opening on the art the previous launch opened on — and calls back when its timeline has run; that
 * callback is where the hand-over pop plays and {@link MainActivity} opens. The version warm-up runs
 * beside the show instead of gating it, so a fast install no longer flashes the art for a moment and
 * a slow one no longer holds a frozen screen.
 *
 * <p>It is the only {@code LAUNCHER} entry, so the hand-over must happen even if the show's clock
 * never reports: a backstop navigates on its own after the show's length plus a margin. A version
 * warm-up failure is reported and the launcher still opens.
 */
public class SplashActivity extends BaseActivity {

    /** Intent extra marking a cold start, used to gate the launcher's arrival animation. */
    public static final String EXTRA_FROM_SPLASH = "from_splash";

    /** Where the last launch's opening slide is remembered, so it is not repeated. */
    private static final String PREFS = "splash_show";
    private static final String KEY_LAST_OPENING = "last_opening";

    /**
     * A splash is a cover, not a destination. If the show's clock never reports, hand over anyway
     * rather than strand the user on the art.
     */
    private static final long BACKSTOP_MARGIN_MS = 1500L;

    private final SplashSound sound = new SplashSound();
    private final MainThread.Scope posts = MainThread.scope();
    private final Runnable backstop = this::navigateToMain;

    private SplashShow show;
    /** This launch's opening slide, by catalogue index, so the next launch can avoid it. */
    private int openingIndex = -1;
    private boolean navigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        show = SplashShow.standard(SplashShow.DEFAULT_SLIDES);
        setContentView(buildSplash());
        sound.prepare(this);
        posts.postDelayed(backstop, show.totalMs() + BACKSTOP_MARGIN_MS);
        warmUpVersions();
    }

    private ComposeView buildSplash() {
        int[] picks = SplashArt.pick(
                SplashArtCatalog.size(), show.slideCount(), new Random(), lastOpening());
        openingIndex = picks.length > 0 ? picks[0] : -1;
        int[] art = new int[picks.length];
        for (int i = 0; i < picks.length; i++) {
            art[i] = SplashArtCatalog.drawableAt(picks[i]);
        }
        ComposeView view = new ComposeView(this);
        view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        CaramelSplashHost.setContent(
                view, art, show, getString(R.string.launch_preparing), this::onShowFinished,
                this::onShowSkipped);
        return view;
    }

    /** The show ran its length: play the hand-over pop and open the launcher. */
    private void onShowFinished() {
        sound.play();
        navigateToMain();
    }

    /** The user skipped the show: the same hand-over, without waiting out the clock. */
    private void onShowSkipped() {
        sound.play();
        navigateToMain();
    }

    private void warmUpVersions() {
        VersionManager.initializeAsync(this, new VersionManager.InitCallback() {
            @Override
            public void onReady(VersionManager manager) {
                // The show is the clock; the warm-up only has to finish before the launcher opens.
            }

            @Override
            public void onFailed(Throwable error) {
                AppLog.warnOnce("SplashActivity", "warmup", "Version warm-up failed", error);
            }
        });
    }

    private void navigateToMain() {
        if (navigated || isFinishing()) return;
        navigated = true;
        posts.remove(backstop);
        rememberOpening();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_FROM_SPLASH, true);
        // Forward a share that arrived on the splash so MainActivity still sees it.
        if (getIntent() != null && getIntent().getData() != null) {
            intent.setData(getIntent().getData());
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.splash_handover_in, R.anim.splash_handover_out);
    }

    /** The slide the previous launch opened on, or -1 when there is none recorded yet. */
    private int lastOpening() {
        try {
            return prefs().getInt(KEY_LAST_OPENING, -1);
        } catch (Throwable error) {
            AppLog.warnOnce("SplashActivity", "last-opening", "could not read the splash history", error);
            return -1;
        }
    }

    /**
     * Records this launch's opening slide.
     *
     * <p>The value is the slide's index in the catalogue, so the next launch can avoid it; a
     * resource id would stop matching the moment the catalogue order changed.
     */
    private void rememberOpening() {
        if (openingIndex < 0) return;
        try {
            prefs().edit().putInt(KEY_LAST_OPENING, openingIndex).apply();
        } catch (Throwable error) {
            AppLog.warnOnce("SplashActivity", "remember-opening", "could not save the splash history", error);
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        posts.close();
        sound.release();
    }
}
