package org.chimeramc.client.ui.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.chimeramc.client.util.ApiLevels;

/**
 * Minimal base for the launcher's full-screen activities.
 *
 * <p>Placeholder left after the legacy UI was removed: it keeps only the immersive-mode plumbing
 * the game-host screens ({@code MinecraftLoadingActivity}, {@code LauncherRestartActivity}) rely on.
 * The previous navigation-bar/rail injection lived here and went with the rest of the shell; a
 * screen that wants chrome now provides its own.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Whether this screen opts out of launcher chrome. Retained so the game-host screens compile;
     * with the navigation shell removed every screen is effectively chrome-free.
     *
     * <p>It also decides whether the system bars are hidden. Most screens are chrome-free and hide
     * them; the launch screen returns {@code false} so the bars stay as the user's escape hatch from
     * a launch that hangs.
     */
    protected boolean shouldSkipNavBar() {
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldSkipNavBar()) {
            hideSystemUI();
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                    visibility -> getWindow().getDecorView().post(this::hideSystemUI));
        } else {
            showSystemUI();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (shouldSkipNavBar()) {
                hideSystemUI();
            } else {
                showSystemUI();
            }
        }
    }

    /**
     * Shows the status and navigation bars.
     *
     * <p>The counterpart to {@link #hideSystemUI()}, for a screen that must not trap the user behind
     * a hidden bar. The bars are the only reliable way out of a screen whose main thread is blocked,
     * so a screen that runs long, uninterruptible work keeps them.
     */
    @SuppressLint("NewApi") // the guard is ApiLevels.atLeastR(), which lint cannot see through
    protected void showSystemUI() {
        View decorView = getWindow().getDecorView();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (ApiLevels.atLeastR()) {
            showBarsOnApi30(decorView);
        }
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    /** Hides the status and navigation bars; transient bars appear on a swipe. */
    @SuppressLint("NewApi") // the guard is ApiLevels.atLeastR(), which lint cannot see through
    protected void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (ApiLevels.atLeastQ()) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (ApiLevels.atLeastR()) {
            hideBarsOnApi30(decorView);
        }
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    /**
     * The API 30 bar controls, kept out of the callers so lint can see the requirement: a
     * {@code Build.VERSION.SDK_INT} check routed through a helper is invisible to it, and the calls
     * would otherwise be reported as new-API errors on a minSdk 28 build.
     */
    @RequiresApi(30)
    private void showBarsOnApi30(View decorView) {
        getWindow().setDecorFitsSystemWindows(true);
        WindowInsetsController controller = decorView.getWindowInsetsController();
        if (controller != null) {
            controller.show(WindowInsets.Type.statusBars()
                    | WindowInsets.Type.navigationBars());
        }
    }

    @RequiresApi(30)
    private void hideBarsOnApi30(View decorView) {
        getWindow().setDecorFitsSystemWindows(false);
        WindowInsetsController controller = decorView.getWindowInsetsController();
        if (controller != null) {
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsets.Type.statusBars()
                    | WindowInsets.Type.navigationBars());
        }
    }
}
