package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import org.chimeramc.client.databinding.ActivityAboutBinding;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.ui.animation.DynamicAnim;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.core.backend.GameBridge;
import org.chimeramc.client.preloader.BerryApi;

public class AboutActivity extends BaseActivity {

    private static final String TAG = "AboutActivity";

    private static final String URL_REPO = "https://github.com/ChimeraAnt-DEV/GlowberryClient";
    private static final String URL_ORG = "https://github.com/ChimeraAnt-DEV";
    private static final String URL_ISSUES = "https://github.com/ChimeraAnt-DEV/GlowberryClient/issues";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Submits the decorative avatar fetch, tolerating an executor {@link #onDestroy()} has shut down.
     *
     * <p>{@link #loadAvatars()} is called from {@code onCreate} but a configuration change tears the
     * old instance down while a fresh one starts up, and submitting to the shut-down pool throws on
     * the calling (main) thread.
     */
    private void runOnExecutor(Runnable work) {
        try {
            executor.execute(work);
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            android.util.Log.w("AboutActivity", "Avatar fetch dropped: the screen is gone", rejected);
        }
    }
    private final OkHttpClient client = buildLatencyTunedClient();

    private static OkHttpClient buildLatencyTunedClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        org.chimeramc.client.settings.LowLatencyNetworkManager.configure(builder);
        return builder.build();
    }

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavBar();

        loadAvatars();
        setupLinks();
        styleBadges();
        showBackendStatus();

        DynamicAnim.applyPressScaleRecursively(binding.getRoot());
    }

    private void styleBadges() {
        TextView ownerBadge = binding.ownerBadge;
        TextView leadBadge = binding.leadBadge;

        org.chimeramc.client.util.PersonalizationManager pm = new org.chimeramc.client.util.PersonalizationManager(this);
        int accent = pm.getAccentColor();
        if (accent != 0) {
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            gd.setColor(accent);
            gd.setCornerRadius(4 * getResources().getDisplayMetrics().density);
            if (ownerBadge != null) {
                ownerBadge.setBackground(gd);
                ownerBadge.setTextColor(android.graphics.Color.WHITE);
            }
            if (leadBadge != null) {
                leadBadge.setBackground(gd);
                leadBadge.setTextColor(android.graphics.Color.WHITE);
            }
        }
    }

    /**
     * Shows which script/runtime backend the launcher found.
     *
     * <p>BerryAPI is a separate native library, so the About page reports it rather than assuming it
     * is present: with none registered the line says so, and the script-driven features are known to
     * be inactive before a launch rather than after a silent no-op.
     */
    private void showBackendStatus() {
        // Name the exact build (version, commit, type) and the native library status, so a
        // screenshot of this screen identifies the revision and says whether the preloader loaded.
        TextView buildInfo = binding.aboutBuildInfo;
        if (buildInfo != null) {
            buildInfo.setText(getString(R.string.about_build_info,
                    org.chimeramc.client.launcher.util.BuildSummary.describe(
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                            BuildConfig.GIT_COMMIT,
                            BuildConfig.BUILD_TYPE,
                            BuildConfig.IS_BETA)));
        }
        TextView nativeStatus = binding.aboutNativeStatus;
        if (nativeStatus != null) {
            nativeStatus.setText(org.chimeramc.client.launcher.util.BuildSummary.nativeStatus(
                    org.chimeramc.client.preloader.PreloaderInput.EXPECTED_ABI_VERSION,
                    org.chimeramc.client.preloader.PreloaderInput.abiVersion()));
        }

        TextView status = binding.aboutBackendStatus;
        if (status != null) {
            status.setText(getString(R.string.about_backend_status, BerryApi.summary()));
        }
        // The input backend is a second, separate seam: the runtime can be present while nothing is
        // registered to inject input through it. Both lines are shown so "the launcher runs but the
        // controls do nothing" is a state the user can name before reporting it.
        TextView inputStatus = binding.aboutInputBackendStatus;
        if (inputStatus != null) {
            inputStatus.setText(getString(R.string.about_input_backend_status,
                    GameBridge.statusLine()));
        }
    }

    private void setupNavBar() {
        // About is reached from Settings, so the Settings tab stays highlighted while it is on
        // screen; there is no About tab any more.
        setActiveNavTab(R.id.nav_tab_settings);
    }

    /**
     * Fetches the two credited people's avatars.
     *
     * <p>The roster owns the handles, so the avatar URL is the profile's own
     * ({@code github.com/<handle>.png}) rather than a hardcoded numeric id that a rename would break.
     * The fetch is decorative, so a failure leaves the placeholder in place.
     */
    private void loadAvatars() {
        org.chimeramc.client.launcher.core.about.AboutPeople.Person owner =
                org.chimeramc.client.launcher.core.about.AboutPeople.of(
                        org.chimeramc.client.launcher.core.about.AboutPeople.Role.OWNER);
        org.chimeramc.client.launcher.core.about.AboutPeople.Person lead =
                org.chimeramc.client.launcher.core.about.AboutPeople.of(
                        org.chimeramc.client.launcher.core.about.AboutPeople.Role.LEAD_DEVELOPER);
        runOnExecutor(() -> {
            loadAvatar(binding.ownerAvatar, owner == null ? null : owner.avatarUrl, "about-owner-avatar");
            loadAvatar(binding.leadAvatar, lead == null ? null : lead.avatarUrl, "about-lead-avatar");
        });
    }

    /**
     * Loads one avatar into {@code target}.
     *
     * <p>try-with-resources on the response, not a bare execute(): an OkHttp response holds a
     * connection until its body is closed, and a swallowed exception left it checked out of the pool.
     */
    private void loadAvatar(com.microsoft.xbox.idp.toolkit.CircleImageView target, String url, String key) {
        if (target == null || url == null || url.isEmpty()) return;
        try (Response resp = client.newCall(new Request.Builder().url(url).build()).execute()) {
            if (resp.isSuccessful() && resp.body() != null) {
                Bitmap bmp = BitmapFactory.decodeStream(resp.body().byteStream());
                runOnUiThread(() -> {
                    if (bmp != null) target.setImageBitmap(bmp);
                });
            }
        } catch (Exception ignored) {
            AppLog.warnOnce(TAG, key, "Fetching a contributor avatar failed", ignored);
        }
    }

    /**
     * Wires every link, including the name and avatar taps, to the roster's own URLs.
     *
     * <p>The name and the avatar open GitHub as well as the explicit chip, because a profile picture
     * is the thing a finger aims at.
     */
    private void setupLinks() {
        org.chimeramc.client.launcher.core.about.AboutPeople.Person owner =
                org.chimeramc.client.launcher.core.about.AboutPeople.of(
                        org.chimeramc.client.launcher.core.about.AboutPeople.Role.OWNER);
        org.chimeramc.client.launcher.core.about.AboutPeople.Person lead =
                org.chimeramc.client.launcher.core.about.AboutPeople.of(
                        org.chimeramc.client.launcher.core.about.AboutPeople.Role.LEAD_DEVELOPER);

        if (owner != null) {
            setupLinkButton(binding.btnOwnerGithub, owner.githubUrl);
            setupLinkButton(binding.btnOwnerYoutube, owner.youtubeUrl);
            setupLinkButton(binding.ownerName, owner.githubUrl);
            setupLinkButton(binding.ownerAvatar, owner.githubUrl);
        }
        if (lead != null) {
            setupLinkButton(binding.btnLeadGithub, lead.githubUrl);
            setupLinkButton(binding.leadName, lead.githubUrl);
            setupLinkButton(binding.leadAvatar, lead.githubUrl);
        }

        setupLinkButton(binding.btnGithubRepo, URL_REPO);
        setupLinkButton(binding.btnGithubOrg, URL_ORG);
        setupLinkButton(binding.btnIssues, URL_ISSUES);
        setupLinkButton(binding.btnStarFork, URL_REPO);
    }

    private void setupLinkButton(android.view.View btn, String url) {
        if (btn == null || url == null || url.isEmpty()) return;
        btn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
                AppLog.warnOnce(TAG, "about-link", "Opening a link failed", ignored);
            }
        });
        DynamicAnim.applyPressScale(btn);
    }

    @Override
    protected void onDestroy() {
        // The avatar fetch runs on an Activity-scoped executor; without this its thread outlives
        // the screen and the process keeps it alive until the next fetch happens to reuse it.
        executor.shutdownNow();
        super.onDestroy();
    }
}
