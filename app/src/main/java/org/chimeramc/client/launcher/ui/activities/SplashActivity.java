package org.chimeramc.client.ui.activities;

import org.chimeramc.client.util.AppLog;

import org.chimeramc.client.util.ApiLevels;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ActivitySplashBinding;
import org.chimeramc.client.launcher.ui.splash.SplashBackgroundRegistry;
import org.chimeramc.client.launcher.ui.splash.SplashInit;
import org.chimeramc.client.launcher.ui.splash.SplashTimeline;
import org.chimeramc.client.util.PersonalizationManager;

import java.util.Locale;

/**
 * The splash sequence: a random Minecraft key-art background, the logo lockup, and a smooth circle
 * rotating in the bottom-right while the launcher warms up.
 *
 * <p>This replaces the 3D scene (floating island, ore block, pickaxe swing, block-break, wordmark
 * shatter). That sequence drew a particle system, a terrain mesh and several animated sprites every
 * frame on the cold-start path, for a screen visible for a couple of seconds. A still image plus one
 * rotating arc is one decode and one draw call.
 *
 * <p>The warm-up is still <b>time-budgeted, not time-fixed</b>. Progress comes from real milestones
 * ({@link SplashInit}) and the display is {@code min(real, paced)} ({@link SplashTimeline}), so a
 * fast start still gets a watchable beat and a slow one holds at its current progress instead of
 * looping or looking stuck. App usability is never blocked behind a fixed-length animation.
 *
 * <p>The screen is also honest about failure: every warm-up step is best-effort, so a storage or
 * network error still lands the user in the launcher.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    /** Intent extra marking a cold start from the splash, used to gate the arrival animation. */
    public static final String EXTRA_FROM_SPLASH = "from_splash";

    private ActivitySplashBinding binding;
    private boolean navigated;
    private boolean completing;
    private boolean revealed;
    private long sequenceStart;

    private ValueAnimator breatheAnimator;
    private Runnable tick;
    private Runnable holdIdle;

    /** The latest real fraction reported by init; the display is derived from this, never from itself. */
    private volatile float realProgress;

    @Override
    protected boolean shouldSkipNavBar() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        syncSystemLocale();
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applySplashTheme();

        // The background is chosen once per launch and never repeats the previous one.
        binding.splashBackground.setImageResource(
                SplashBackgroundRegistry.drawableFor(
                        SplashBackgroundRegistry.pickForThisLaunch(this)));

        binding.imgLeaf.setAlpha(0f);
        binding.imgLeaf.setScaleX(0.8f);
        binding.imgLeaf.setScaleY(0.8f);
        binding.logoGlow.setAlpha(0f);
        binding.logoGlow.setScaleX(0.5f);
        binding.logoGlow.setScaleY(0.5f);
        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationX(60f);
        binding.tvPreparing.setAlpha(0f);

        binding.getRoot().post(this::startSplashSequence);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAnimators();
    }

    private void cancelAnimators() {
        if (breatheAnimator != null) breatheAnimator.cancel();
        if (tick != null) binding.getRoot().removeCallbacks(tick);
        if (holdIdle != null) binding.getRoot().removeCallbacks(holdIdle);
    }

    // ---------------------------------------------------------------- Part 1.1: lockup entrance

    private void startSplashSequence() {
        sequenceStart = SystemClock.uptimeMillis();
        boolean animate = new PersonalizationManager(this).isShowAnimations();

        if (!animate) {
            // Reduced motion: no loader theatre, just warm up and go.
            binding.logoGlow.setAlpha(1f);
            binding.imgLeaf.setAlpha(1f);
            binding.tvAppName.setAlpha(1f);
            binding.tvAppName.setTranslationX(0f);
            binding.tvPreparing.setAlpha(0.7f);
        } else {
            // Logo first, then the wordmark slides in 100ms later, overlapping the logo's tail so
            // the pair reads as one gesture rather than two separate beats.
            binding.logoGlow.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .start();

            binding.imgLeaf.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .start();

            binding.tvAppName.animate()
                    .alpha(1f).translationX(0f)
                    .setStartDelay(100)
                    .setDuration(250)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .start();

            binding.tvPreparing.animate()
                    .alpha(0.7f)
                    .setStartDelay(260)
                    .setDuration(200)
                    .start();

            startLogoBreathe();
        }

        runInit(animate);
    }

    /**
     * A slow scale/alpha breathe on the halo, so the lockup keeps a pulse of life the whole time
     * the loader is cracking. Without it the mark sits perfectly still after its entrance, which
     * reads as a frozen frame on a slow warm-up.
     */
    private void startLogoBreathe() {
        breatheAnimator = ValueAnimator.ofFloat(0f, 1f);
        breatheAnimator.setDuration(2400);
        breatheAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breatheAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breatheAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        // Start after the entrance has settled the halo at alpha 1 / scale 1, so the two animators
        // do not fight over the same properties during the first beat.
        breatheAnimator.setStartDelay(620);
        breatheAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float scale = 1f + 0.055f * t;
            binding.logoGlow.setScaleX(scale);
            binding.logoGlow.setScaleY(scale);
            binding.logoGlow.setAlpha(0.9f + 0.1f * t);
        });
        breatheAnimator.start();
    }

    // ---------------------------------------------------- Part 1.2/1.3: real progress + hold state

    /**
     * Wires real init to the loader.
     *
     * @param animate false on the reduced-motion path, which skips straight to the launcher once
     *                the work is done instead of running the wind-up and wipe.
     */
    private void runInit(final boolean animate) {
        SplashInit.run(this, new SplashInit.Listener() {
            @Override
            public void onProgress(float fraction) {
                realProgress = fraction;
            }

            @Override
            public void onComplete() {
                realProgress = 1f;
                initComplete = true;
            }
        });

        if (animate) {
            startTicking();
        } else {
            // Poll only for completion; there is no loader animation to advance.
            Runnable wait = new Runnable() {
                @Override
                public void run() {
                    if (initComplete) {
                        navigateToMain();
                    } else if (!navigated) {
                        binding.getRoot().postDelayed(this, 50L);
                    }
                }
            };
            binding.getRoot().postDelayed(wait, 50L);
        }
    }

    private boolean initComplete;

    /** Advances the visible loader on a fixed tick while init is in flight. */
    private void startTicking() {
        tick = new Runnable() {
            @Override
            public void run() {
                if (navigated) return;
                long elapsed = SystemClock.uptimeMillis() - sequenceStart;
                float real = realProgress;

                // The spinner turns continuously, so there is no per-tick progress to paint; the
                // timeline only decides when the warm-up is done.
                if (SplashTimeline.isLoadComplete(real, elapsed)) {
                    finishSequence();
                    return;
                }

                if (SplashTimeline.isHolding(real, elapsed)) {
                    // Held: stop the fast tick and idle-swing so a slow init does not look stuck.
                    scheduleHoldIdle();
                } else {
                    binding.getRoot().postDelayed(this, 50L);
                }
            }
        };
        binding.getRoot().postDelayed(tick, 50L);
    }

    /** A small re-swing every {@link SplashTimeline#HOLD_IDLE_MS} while waiting on slow init. */
    private void scheduleHoldIdle() {
        if (navigated || holdIdle != null) return;
        holdIdle = new Runnable() {
            @Override
            public void run() {
                holdIdle = null;
                if (navigated) return;
                long elapsed = SystemClock.uptimeMillis() - sequenceStart;
                float real = realProgress;
                if (SplashTimeline.isLoadComplete(real, elapsed)) {
                    finishSequence();
                } else if (SplashTimeline.isHolding(real, elapsed)) {
                    binding.getRoot().postDelayed(this, SplashTimeline.HOLD_IDLE_MS);
                } else {
                    binding.getRoot().postDelayed(tick, 50L);
                }
            }
        };
        binding.getRoot().postDelayed(holdIdle, SplashTimeline.HOLD_IDLE_MS);
    }

    // ------------------------------------------------------------------ completion sequence

    /**
     * The warm-up is done: fade out and hand over to the launcher.
     *
     * <p>There is no wind-up, pickaxe travel or wordmark shatter any more — those existed to make
     * the old loader's completion readable. A short fade is the whole completion now, and it cannot
     * leave the user staring at a frozen frame if an animator misbehaves, because the navigation is
     * also scheduled on a plain handler as a backstop.
     */
    private void finishSequence() {
        if (navigated || completing) return;
        completing = true;

        if (holdIdle != null) {
            binding.getRoot().removeCallbacks(holdIdle);
            holdIdle = null;
        }
        if (tick != null) {
            binding.getRoot().removeCallbacks(tick);
            tick = null;
        }

        binding.getRoot().animate()
                .alpha(0f)
                .setDuration(220L)
                .withEndAction(this::navigateToMain)
                .start();

        // Backstop: if the animator never ends (a detached window, a dropped frame storm), still
        // navigate rather than stranding the user on the splash.
        binding.getRoot().postDelayed(() -> {
            if (!navigated) navigateToMain();
        }, 700L);
    }

    // ------------------------------------------------------------------------------- navigation

    private void navigateToMain() {
        if (navigated) return;
        navigated = true;

        // A first run goes to the dashboard, where MainActivity opens the first-run wizard as a
        // self-sizing popup over it. The wizard is no longer a splash destination of its own: it is
        // an overlay, so the launcher is laid out behind it. SetupWizardActivity.shouldShow owns
        // whether it is due, and MainActivity asks it once.
        //
        // Otherwise the launcher reopens on the destination the user was last on, so a
        // cold start does not always reset to Play. An absent or unknown stored name falls back to
        // Play, which is what BaseActivity.rememberedTab() returning null means.
        Class<?> destination;
        if (org.chimeramc.client.ui.activities.SetupWizardActivity.shouldShow(SplashActivity.this)) {
            destination = MainActivity.class;
        } else {
            org.chimeramc.client.ui.navigation.LauncherTab remembered = BaseActivity.rememberedTab();
            destination = remembered != null ? remembered.activity() : MainActivity.class;
        }
        Intent intent = new Intent(SplashActivity.this, destination);
        // The arrival animation is gated on this extra so it plays once per cold start and never
        // replays when the user simply returns to the Launch tab.
        intent.putExtra(EXTRA_FROM_SPLASH, true);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ---------------------------------------------------------------------------------- theming

    private void applySplashTheme() {
        int accent = resolveAccentColor();
        boolean dark = isDarkMode();
        binding.tvAppName.setTextColor(accent);
        // The Glowberry mark is full-colour art, so it keeps its own palette; only the wordmark,
        // the halo and the backdrop follow the user's accent.
        binding.imgLeaf.setImageTintList(null);
        binding.logoGlow.setBackground(createRadialGlow(accent));
        binding.tvPreparing.setTextColor(blendColors(
                getColor(R.color.text_secondary),
                accent,
                dark ? 0.24f : 0.18f
        ));
        binding.splashSpinner.setAccentColor(accent);
    }

    private int resolveAccentColor() {
        int accent = new PersonalizationManager(this).getAccentColor();
        return accent != 0 ? accent : getColor(R.color.primary);
    }

    private GradientDrawable createRadialGlow(int accent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setGradientRadius(dp(90));
        drawable.setColors(new int[]{
                withAlpha(accent, isDarkMode() ? 42 : 30),
                withAlpha(accent, isDarkMode() ? 18 : 14),
                Color.TRANSPARENT
        });
        return drawable;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
                Math.max(0, Math.min(alpha, 255)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private int blendColors(int from, int to, float ratio) {
        float boundedRatio = Math.max(0f, Math.min(ratio, 1f));
        float inverse = 1f - boundedRatio;
        return Color.rgb(
                (int) (Color.red(from) * inverse + Color.red(to) * boundedRatio),
                (int) (Color.green(from) * inverse + Color.green(to) * boundedRatio),
                (int) (Color.blue(from) * inverse + Color.blue(to) * boundedRatio)
        );
    }

    private boolean isDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(float value) {
        return Math.max(1, Math.round(value * getResources().getDisplayMetrics().density));
    }

    private void syncSystemLocale() {
        if (ApiLevels.atLeastTiramisu()) {
            try {
                android.app.LocaleManager localeManager = getSystemService(android.app.LocaleManager.class);
                if (localeManager != null) {
                    android.os.LocaleList appLocales = localeManager.getApplicationLocales();
                    android.os.LocaleList systemLocales = localeManager.getSystemLocales();
                    if (!appLocales.isEmpty()) {
                        localeManager.setApplicationLocales(android.os.LocaleList.getEmptyLocaleList());
                    }
                    if (!systemLocales.isEmpty()) {
                        Locale.setDefault(systemLocales.get(0));
                    }
                }
            } catch (Exception ignored) {
                AppLog.warnOnce("GlowberryClient", "SplashActivity:549", "Ignored a failure at SplashActivity:549", ignored);
            }
        }
    }
}
