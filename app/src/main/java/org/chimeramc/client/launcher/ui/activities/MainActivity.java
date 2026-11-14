package org.chimeramc.client.ui.activities;

import org.chimeramc.client.util.AppLog;

import org.chimeramc.client.util.ApiLevels;

import org.chimeramc.client.util.SafeExecutor;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Looper;
import android.view.ViewOutlineProvider;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.core.minecraft.MinecraftImportIntents;
import org.chimeramc.client.core.minecraft.GamePackageManager;
import org.chimeramc.client.launcher.core.minecraft.LaunchFailurePresenter;
import org.chimeramc.client.launcher.core.minecraft.AndroidLaunchProbe;
import org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis;
import org.chimeramc.client.launcher.core.minecraft.LaunchProbe;
import org.chimeramc.client.launcher.core.minecraft.LaunchReadiness;
import org.chimeramc.client.preloader.SweetLoaderSignatureRulesManager;
import org.chimeramc.client.core.minecraft.LaunchTrace;
import org.chimeramc.client.core.minecraft.MinecraftLauncher;
import org.chimeramc.client.core.minecraft.PlaytimeManager;
import org.chimeramc.client.core.mods.FileHandler;
import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.changelog.ChangelogManager;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.ui.components.ListDiff;
import org.chimeramc.client.ui.components.ListDiffDispatch;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.databinding.ActivityMainBinding;
import org.chimeramc.client.settings.FeatureSettings;

import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.animation.LaunchIntro;
import org.chimeramc.client.ui.dialogs.CustomAlertDialog;
import org.chimeramc.client.ui.dialogs.LibsRepairDialog;
import org.chimeramc.client.ui.dialogs.MinecraftNotFoundDialog;
import org.chimeramc.client.ui.views.MainViewModel;
import org.chimeramc.client.ui.views.MainViewModelFactory;
import org.chimeramc.client.util.ApkImportManager;
import org.chimeramc.client.util.GithubReleaseUpdater;
import org.chimeramc.client.util.LanguageManager;
import org.chimeramc.client.util.InstanceShortcutManager;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.util.PermissionsHandler;
import org.chimeramc.client.launcher.util.IconSizeApplier;
import org.chimeramc.client.util.PersonalizationManager;
import org.chimeramc.client.util.PlayStoreValidator;
import org.chimeramc.client.util.ResourcepackHandler;
import org.chimeramc.client.util.StorageMigrationManager;
import org.chimeramc.client.util.StorageMigrationService;
import org.chimeramc.client.util.UIHelper;
import org.chimeramc.client.core.content.ContentManager;
import org.chimeramc.client.util.MainThread;
import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;


 import android.widget.Button;
 import android.widget.ProgressBar;
 import android.graphics.Bitmap;
 import android.view.Gravity;
 import android.widget.PopupWindow;
 import android.view.LayoutInflater;
 import android.graphics.drawable.ColorDrawable;
 import android.util.TypedValue;
 import android.view.ViewGroup;
 import android.view.ViewTreeObserver;
 import androidx.core.content.ContextCompat;

import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import okhttp3.OkHttpClient;
 import okhttp3.Request;
 import okhttp3.Response;

 import org.chimeramc.client.core.auth.MsftAccountStore;
 import org.chimeramc.client.core.auth.MsftAuthManager;
 import org.chimeramc.client.ui.dialogs.LoadingDialog;
 import org.chimeramc.client.util.AccountTextUtils;
 import org.chimeramc.client.util.DialogUtils;

 import static org.chimeramc.client.core.minecraft.MinecraftProcessRestarterKt.ACTION_MAIN_ACTIVITY_FIRST_DRAWN;
 import static org.chimeramc.client.core.minecraft.MinecraftProcessRestarterKt.EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW;

 public class MainActivity extends BaseActivity {

    private static final long TIP_ROTATE_MS = 8000L;

    /** The tip currently shown, or -1 before the first paint. */
    private int tipIndex = -1;

    private ActivityMainBinding binding;
    private MinecraftLauncher minecraftLauncher;
    private LanguageManager languageManager;
    private PermissionsHandler permissionsHandler;
    private FileHandler fileHandler;
    private ApkImportManager apkImportManager;
    private MainViewModel viewModel;
    private VersionManager versionManager;
    private StorageMigrationManager storageMigrationManager;
    private ActivityResultLauncher<Intent> permissionResultLauncher;
    private ActivityResultLauncher<Intent> apkImportResultLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private ContentManager contentManager;
    private TextView worldsCountText;
    private TextView resourcePacksCountText;
    private TextView behaviorPacksCountText;

    private com.microsoft.xbox.idp.toolkit.CircleImageView accountAvatar;
    private View accountAvatarContainer;
    private ProgressBar avatarProgress;
    private Button signInButton;
    private boolean headerAccountViewsLoaded;
    private com.microsoft.xbox.idp.toolkit.CircleImageView headerAccountAvatar;
    private TextView headerAccountName;
    private Button headerSignInButton;
    private String lastAvatarXuid;
    private final OkHttpClient avatarClient = MainActivity.buildLatencyTunedClient();

    private static OkHttpClient buildLatencyTunedClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        org.chimeramc.client.settings.LowLatencyNetworkManager.configure(builder);
        return builder.build();
    }
    private ExecutorService accountExecutor = Executors.newSingleThreadExecutor();
    private LoadingDialog accountLoadingDialog;
    private ActivityResultLauncher<Intent> accountLoginLauncher;
    private OnBackPressedCallback onBackPressedCallback;
    private boolean migrationPromptShown;
    private boolean migrationPromptCheckInFlight;
    private boolean postMigrationInitialized;
    private boolean postEulaFlowRunning;

    /**
     * Whether the first-run wizard overlay has already been opened this process.
     *
     * <p>Static on purpose: the wizard is a translucent overlay on top of this activity, so it
     * must be opened once per app run and never relaunched when this activity is recreated (a theme
     * change recreates it) or resumed (returning from the wizard or the file picker resumes it). A
     * per-instance flag would re-open the wizard after every recreation and could trap the user.
     */
    private static boolean sWizardOverlayLaunched;

    /** Marks the first-run wizard overlay as opened, so it is not opened again this process. */
    static void markSetupWizardLaunched() {
        sWizardOverlayLaunched = true;
    }

    /** Clears the once-per-process guard, used only when the popup was the process's last task. */
    static void resetSetupWizardLaunched() {
        sWizardOverlayLaunched = false;
    }
    private StorageMigrationService storageMigrationService;
    private boolean storageMigrationBound;
    private LibsRepairDialog storageMigrationDialog;
    private StorageMigrationService.MigrationState lastMigrationState;
    private final ExecutorService storageMigrationExecutor = Executors.newSingleThreadExecutor();

    /**
     * Runs the launch preflight off the UI thread. Separate from the storage-migration executor so
     * a long migration cannot hold up a launch, and so a launch probe cannot delay a migration.
     */
    private final ExecutorService launchProbeExecutor = Executors.newSingleThreadExecutor();

    /**
     * Runs a resource-pack intent's launch. It used to be a fresh executor created inside
     * {@code checkResourcepack()} on every call and never shut down, so each incoming pack intent
     * leaked a thread; it is one executor for the Activity's lifetime now.
     */
    private final ExecutorService resourcepackExecutor = Executors.newSingleThreadExecutor();

    /**
     * Gathers the Launch tab's disk-backed status counts (installed mods, replay clips).
     *
     * <p>Separate from the other pools because it runs on every {@code onResume} and its work is
     * only a couple of directory reads: it must never queue behind a migration or a launch probe,
     * and those must never wait for it.
     */
    private final ExecutorService launchStatsExecutor = Executors.newSingleThreadExecutor();

    private final Handler uiHandler = MainThread.handler();
    /** The announcements feed client, shut down in onDestroy. */
    private org.chimeramc.client.core.announce.AnnouncementClient announcementClient;

    /** The lifetime of the self-reposting tip rotation, closed in onDestroy. */
    private final MainThread.Scope tipScope = MainThread.scope();
    private Runnable pulseRunnable;
    private boolean pulseRunning;
    private View launchShimmer;
    private Runnable shimmerRunnable;
    private boolean shimmerRunning;
    private org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView launchPreview;

    /** The Compose hero host and the state it renders; set up in setupLaunchHero(). */
    private androidx.compose.runtime.MutableState<org.chimeramc.client.ui.compose.launch.LaunchHeroState> heroState;
    /** The preview stage's background state, and the picker host shown on a long-press. */
    private androidx.compose.runtime.MutableState<String> backgroundState;
    private androidx.compose.ui.platform.ComposeView backgroundPickerCompose;
    private androidx.compose.ui.platform.ComposeView heroCompose;
    private String heroName = "";
    private String heroVersion = "";
    private org.chimeramc.client.ui.compose.launch.LaunchHeroState.AbiKind heroAbi =
            org.chimeramc.client.ui.compose.launch.LaunchHeroState.AbiKind.NONE;
    private int heroInstancesCount;
    private int heroModsCount;
    private String heroPlaytime = "0m";
    private androidx.compose.runtime.MutableState<java.util.List<org.chimeramc.client.ui.compose.launch.LaunchToggleState.Toggle>> toggleState;
    private androidx.compose.ui.platform.ComposeView toggleCompose;
    /** The Compose release-notes card and its popup, and the state they render. */
    private androidx.compose.ui.platform.ComposeView changelogCompose;
    private androidx.compose.runtime.MutableState<org.chimeramc.client.ui.compose.launch.LaunchChangelogState> changelogState;
    private androidx.compose.runtime.MutableState<Boolean> changelogOpen;
    private String modsStatusText = "";
    private String voiceStatusText = "";
    private String replayStatusText = "";
    /** True while a launch is in flight, so the hero card and Play button cannot double-fire. */
    private boolean launchInProgress;
    private ValueAnimator activeProgressAnimator;

    private final StorageMigrationService.MigrationListener storageMigrationListener =
            state -> runOnUiThread(() -> handleStorageMigrationState(state));

    private final ServiceConnection storageMigrationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            storageMigrationService = ((StorageMigrationService.LocalBinder) service).getService();
            storageMigrationBound = true;
            storageMigrationService.addListener(storageMigrationListener);
            handleStorageMigrationState(storageMigrationService.getCurrentState());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (storageMigrationService != null) {
                storageMigrationService.removeListener(storageMigrationListener);
            }
            storageMigrationService = null;
            storageMigrationBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        closeLauncherRestartAfterFirstDraw();
        setupNavBar();
        setupManagersAndHandlers();
        if (!isInstanceShortcutIntent()) {
            new GithubReleaseUpdater(this, GithubReleaseUpdater.REPO_OWNER, GithubReleaseUpdater.REPO_NAME, permissionResultLauncher).checkUpdateOnLaunch();
        }
        setupOnBackPressedCallback();

        accountLoginLauncher = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null
                    && result.getData().getBooleanExtra(MsftLoginActivity.EXTRA_LOGIN_COMPLETED, false)) {
                String name = result.getData().getStringExtra(MsftLoginActivity.EXTRA_LOGIN_NAME);
                String statusName = name != null ? name : getString(R.string.not_signed_in);
                Toast.makeText(this, getString(R.string.ms_login_success, statusName), Toast.LENGTH_SHORT).show();
            }
            refreshAccountHeaderUI();
            refreshNavAccountUI();
        });

        initAccountHeader();
        // Auto sign-in: silently refresh the active account's stored token in the background. The
        // Microsoft refresh path works from the saved auth blob regardless of how stale it is; only
        // a genuinely dead/revoked token routes to the sign-in screen (with the reason pre-shown).
        autoRefreshActiveAccount();
        binding.getRoot().post(this::showPostEulaFlow);
        binding.getRoot().post(this::showSetupWizardIfNeeded);
        binding.getRoot().post(this::playArrivalAnimations);
    }

    /**
     * The arrival motion for a cold start launched through the splash.
     *
     * <p>Gated strictly behind the splash extra so it plays once per cold start and never replays
     * when the user returns to the Launch tab: a tab switch recreates this activity, and an
     * animation that re-ran every time would read as the app reloading rather than the user moving.
     */
    private void playArrivalAnimations() {
        Intent intent = getIntent();
        if (intent == null || !intent.getBooleanExtra(SplashActivity.EXTRA_FROM_SPLASH, false)) {
            return;
        }
        // Consume the extra so a configuration change or a return to this tab cannot replay it.
        intent.removeExtra(SplashActivity.EXTRA_FROM_SPLASH);

        animateNavTabsArrival();
        animateHomeCardsArrival();
        animateBlurFadeIn();
    }

    /**
     * Home screen entrance stagger in reading order: account row, hero, toggles, content, dock, then
     * the launch control. The order comes from {@link LaunchIntro} rather than being spelled out
     * here, so a layout reorder cannot desync the eye's path from the animation's.
     *
     * <p>Reduced motion is handled inside {@link DynamicAnim#overshootScaleIn}, which snaps the views
     * straight to full.
     */
    private void animateHomeCardsArrival() {
        for (LaunchIntro.Block block : LaunchIntro.ORDER) {
            View view = viewForIntroBlock(block);
            if (view == null) continue;
            DynamicAnim.overshootScaleIn(view, LaunchIntro.delayFor(block));
        }
    }

    /** The view a reading-order block animates, or null when this screen does not carry it. */
    private View viewForIntroBlock(LaunchIntro.Block block) {
        switch (block) {
            case ACCOUNT:
                return findViewById(R.id.account_status_bar);
            case HERO:
                return findViewById(R.id.launch_hero_compose);
            case TOGGLES:
                return findViewById(R.id.launch_toggles_compose);
            case CONTENT:
                return findViewById(R.id.section_content);
            case DOCK:
                return findViewById(R.id.launch_quickdock);
            case LAUNCH:
                return binding != null && binding.launchDock != null
                        ? binding.launchDock.launchButton : null;
            default:
                return null;
        }
    }

    /**
     * Fades the backdrop in behind the arriving blocks.
     *
     * <p>The blur/dim scrim is painted by {@link #applyBlurIntensity}; this ramps its alpha from 0
     * over the entrance so the world behind the panel settles in rather than appearing at full
     * strength. A screen with no background image has nothing to fade, so it is skipped.
     */
    private void animateBlurFadeIn() {
        if (!DynamicAnim.areAnimationsEnabled()) return;
        if (binding == null || binding.getRoot() == null) return;
        PersonalizationManager pm = new PersonalizationManager(this);
        if (!pm.hasBackgroundImage()) return;
        float target = binding.getRoot().getBackground() == null ? 0f : 1f;
        if (target <= 0f) return;
        ValueAnimator fade = ValueAnimator.ofFloat(0f, 1f);
        fade.setDuration(LaunchIntro.BLUR_FADE_MS);
        fade.setInterpolator(DynamicAnim.getDefaultInterpolator());
        fade.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            binding.getRoot().setAlpha(0.4f + 0.6f * t);
        });
        fade.start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVersionDependentIntent();
    }

    private void closeLauncherRestartAfterFirstDraw() {
        Intent intent = getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW, false)) {
            return;
        }
        intent.removeExtra(EXTRA_CLOSE_RESTART_ACTIVITY_ON_FIRST_DRAW);
        setIntent(intent);

        final View root = binding.getRoot();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (root.getViewTreeObserver().isAlive()) {
                    root.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                root.post(() -> {
                    hideSystemUI();
                    sendBroadcast(new Intent(ACTION_MAIN_ACTIVITY_FIRST_DRAWN).setPackage(getPackageName()));
                });
                return true;
            }
        });
    }


    private void initAccountHeader() {
        signInButton = findViewById(R.id.nav_sign_in_button);
        accountAvatar = findViewById(R.id.nav_account_avatar);
        accountAvatarContainer = findViewById(R.id.nav_account_avatar_container);
        avatarProgress = findViewById(R.id.nav_avatar_progress);

        if (signInButton != null) {
            signInButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, MsftLoginActivity.class);
                accountLoginLauncher.launch(intent);
            });
            DynamicAnim.applyPressScale(signInButton);
        }
        if (accountAvatarContainer != null) {
            accountAvatarContainer.setOnClickListener(this::showAccountSwitchPopup);
            DynamicAnim.applyPressScale(accountAvatarContainer);
        }

        refreshAccountHeaderUI();
    }

    private MsftAccountStore.MsftAccount getActiveAccount() {
        java.util.List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(this);
        for (MsftAccountStore.MsftAccount a : list) if (a.active) return a;
        return null;
    }

    /**
     * Refreshes the active account's Microsoft token silently on the background thread at launch.
     *
     * <p>The working sign-in path is {@link MsftAuthManager#refreshAndAuth}, which replays the stored
     * {@code serializedAuthManager} blob through the Bedrock Android client id. The MSAL silent path
     * can never succeed against a placeholder client id, so the default flow never depends on it.
     * Whatever the account's age, refreshing at launch keeps its certificate chain from expiring
     * mid-session; {@link MsftAuthManager#saveAccountOrThrow} persists the fresh blob so the next
     * launch finds it already current.
     *
     * <p>If the refresh throws, the sign-in screen opens with the reason already visible
     * ({@link org.chimeramc.client.ui.activities.MsftLoginActivity#EXTRA_PRELOADED_ERROR}) so the
     * user does not see a silent no-op.
     */
    private void autoRefreshActiveAccount() {
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null || isFinishing() || isDestroyed()) {
            return;
        }
        final String accountId = active.id;
        runOnAccountExecutor(() -> {
            try {
                BedrockAuthManager authManager = MsftAuthManager.refreshAndAuth(active);
                MsftAuthManager.saveAccountOrThrow(MainActivity.this, authManager);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        refreshAccountHeaderUI();
                        refreshNavAccountUI();
                    }
                });
            } catch (Throwable e) {
                // A stale or revoked token can no longer be refreshed silently. Send the user to the
                // sign-in screen with the failure already explained instead of leaving them signed out
                // with no prompt. Throwable, not Exception: an Error on this worker thread would
                // otherwise die with the prompt never shown.
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                String error = MsftAuthManager.describeError(e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || accountId == null) {
                        return;
                    }
                    MsftAccountStore.MsftAccount stillActive = MsftAccountStore.find(MainActivity.this, accountId);
                    if (stillActive == null || !stillActive.active) {
                        // The user switched accounts while the refresh was running; do not hijack it.
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, MsftLoginActivity.class);
                    intent.putExtra(MsftLoginActivity.EXTRA_PRELOADED_ERROR, error);
                    accountLoginLauncher.launch(intent);
                });
            }
        });
    }

     private void setupOnBackPressedCallback() {
         onBackPressedCallback = new OnBackPressedCallback(true) {
             @Override
             public void handleOnBackPressed() {
                 org.chimeramc.client.ui.dialogs.CustomAlertDialog exitDialog = new org.chimeramc.client.ui.dialogs.CustomAlertDialog(MainActivity.this);
                 exitDialog.setTitleText(getString(org.chimeramc.client.R.string.dialog_title_exit_app))
                         .setMessage(getString(org.chimeramc.client.R.string.dialog_message_exit_app))
                         .setPositiveButton(getString(org.chimeramc.client.R.string.dialog_positive_exit), v -> {
                             exitDialog.dismissImmediately();
                             finishAffinity();
                         })
                         .setNegativeButton(getString(org.chimeramc.client.R.string.dialog_negative_cancel), null)
                         .show();
             }
         };

         getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
     }

    private void refreshAccountHeaderUI() {
        ensureHeaderAccountViews();
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null) {
            if (signInButton != null) signInButton.setVisibility(View.VISIBLE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.GONE);
            if (accountAvatar != null) accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            if (headerSignInButton != null) headerSignInButton.setVisibility(View.VISIBLE);
            if (headerAccountAvatar != null) headerAccountAvatar.setVisibility(View.GONE);
            if (headerAccountName != null) headerAccountName.setText(AccountTextUtils.displayNameOrNotSigned(this, null));
        } else {
            if (signInButton != null) signInButton.setVisibility(View.GONE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.VISIBLE);
            if (headerSignInButton != null) headerSignInButton.setVisibility(View.GONE);
            if (headerAccountAvatar != null) headerAccountAvatar.setVisibility(View.VISIBLE);
            if (headerAccountName != null) headerAccountName.setText(AccountTextUtils.displayNameOrNotSigned(this, active));
            loadXboxAvatar(active);
            if (headerAccountAvatar != null && accountAvatar != null && accountAvatar.getDrawable() != null) {
                headerAccountAvatar.setImageDrawable(accountAvatar.getDrawable());
            }
        }
    }

    private void ensureHeaderAccountViews() {
        if (headerAccountViewsLoaded) return;
        headerAccountAvatar = findViewById(R.id.header_account_avatar);
        headerAccountName = findViewById(R.id.header_account_name);
        headerSignInButton = findViewById(R.id.header_sign_in_button);
        headerAccountViewsLoaded = true;
    }

    private void loadXboxAvatar(MsftAccountStore.MsftAccount active) {
        if (accountAvatar == null) return;
        String url = AccountTextUtils.sanitizeUrl(active != null ? active.xboxAvatarUrl : null);
        if (url == null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            return;
        }

        Object currentUrl = accountAvatar.getTag(R.id.nav_account_avatar);
        if (url.equals(currentUrl) && accountAvatar.getDrawable() != null) {
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            return;
        }

        Bitmap cached = AccountTextUtils.getCachedAvatar(url);
        if (cached != null) {
            accountAvatar.setTag(R.id.nav_account_avatar, url);
            accountAvatar.setImageBitmap(cached);
            if (headerAccountAvatar != null) headerAccountAvatar.setImageBitmap(cached);
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            return;
        }

        accountAvatar.setTag(R.id.nav_account_avatar, url);
        accountAvatar.setImageDrawable(null);
        if (avatarProgress != null) avatarProgress.setVisibility(View.VISIBLE);
        runOnAccountExecutor(() -> {
            try {
                try (Response imgResp = avatarClient.newCall(new Request.Builder().url(url).build()).execute()) {
                    Bitmap bmp = (imgResp.isSuccessful() && imgResp.body() != null) ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream()) : null;
                    runOnUiThread(() -> {
                        if (!url.equals(accountAvatar.getTag(R.id.nav_account_avatar))) return;
                        if (bmp != null) {
                            AccountTextUtils.cacheAvatar(url, bmp);
                            accountAvatar.setImageBitmap(bmp);
                            if (headerAccountAvatar != null) headerAccountAvatar.setImageBitmap(bmp);
                        }
                        if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                    });
                }
            } catch (Throwable e) {
                // Throwable, not Exception: decoding the avatar can exhaust the heap and that is an
                // Error, which skipped this handler and left the account spinner turning forever
                // with no way to tell it had failed.
                runOnUiThread(() -> {
                    if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    /**
     * Shows the account switcher popup.
     *
     * <p>The popup itself lives in {@link AccountSwitchPopup}; this method only builds it with the
     * Activity's own side effects (the account executor, the shared loading dialog and the header
     * refresh), so the Launch tab keeps its headroom under the file-size budget.
     */
    private void showAccountSwitchPopup(View anchor) {
        new AccountSwitchPopup(this, accountSwitchHost).show(anchor);
    }

    /** The side effects {@link AccountSwitchPopup} cannot perform for itself. */
    private final AccountSwitchPopup.Host accountSwitchHost = new AccountSwitchPopup.Host() {
        @Override
        public void runOnAccountExecutor(Runnable work) {
            MainActivity.this.runOnAccountExecutor(work);
        }

        @Override
        public LoadingDialog loadingDialog() {
            return accountLoadingDialog;
        }

        @Override
        public void setLoadingDialog(LoadingDialog dialog) {
            accountLoadingDialog = dialog;
        }

        @Override
        public void refreshHeader() {
            refreshAccountHeaderUI();
        }

        @Override
        public android.graphics.drawable.Drawable headerAvatarDrawable() {
            return accountAvatar != null ? accountAvatar.getDrawable() : null;
        }
    };


    private void setupManagersAndHandlers() {
        languageManager = new LanguageManager(this);
        languageManager.applySavedLanguage();
        storageMigrationManager = new StorageMigrationManager(this);
        permissionResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (permissionsHandler != null)
                        permissionsHandler.onActivityResult(result.getResultCode(), result.getData());
                }
        );
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                }
        );
        apkImportResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (apkImportManager != null)
                        apkImportManager.handleActivityResult(result.getResultCode(), result.getData());
                }
        );
        permissionsHandler = PermissionsHandler.getInstance();
        permissionsHandler.setActivity(this, permissionResultLauncher);
        initListeners();
    }

    private void initializeAfterMigrationGate() {
        if (postMigrationInitialized || isFinishing() || isDestroyed()) return;
        postMigrationInitialized = true;

        minecraftLauncher = new MinecraftLauncher(this);
        viewModel = new ViewModelProvider(this, new MainViewModelFactory(getApplication())).get(MainViewModel.class);
        apkImportManager = new ApkImportManager(this, viewModel);

        initializeVersionManager();
        initModsSection();
        initContentManagementSection();
        initMiscellaneousSection();
    }

    private void initializeVersionManager() {
        setLaunchControlEnabled(false);
        versionManager = VersionManager.getIfInitialized();
        if (versionManager != null) {
            onVersionManagerReady();
            return;
        }
        VersionManager.initializeAsync(this, new VersionManager.InitCallback() {
            @Override
            public void onReady(VersionManager manager) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    versionManager = manager;
                    onVersionManagerReady();
                });
            }

            @Override
            public void onFailed(Throwable error) {
                // The Launch control was disabled above and only onVersionManagerReady re-arms it,
                // so a failed build has to release it here or the Play button stays dead for the
                // life of the screen. The cause is named through the same classifier the rest of
                // the launch path uses.
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    resetLaunchButton();
                    reportLaunchFailure(error);
                });
            }
        });
    }

    private void onVersionManagerReady() {
        if (versionManager == null || binding == null) return;
        fileHandler = new FileHandler(this, viewModel, versionManager);
        resetLaunchButton();
        if (!handleInstanceShortcutLaunch()) {
            setTextMinecraftVersion();
            updateViewModelVersion();
            repairNeededVersions();
            handleVersionDependentIntent();
        }
        refreshContentCounts();
        // After the launcher has settled, honour a relaunch the in-game pack changer requested.
        // It is a runnable (not a plain boolean) because the request must survive the process
        // restart the relaunch itself performs: the flag is static, so the freshly started
        // launcher reads it here and re-launches the instance the player was in.
        binding.getRoot().post(this::maybeRelaunchForPendingRestart);
    }

    /**
     * Re-launches the instance after the in-game pack changer applied a change.
     *
     * <p>The changer writes the pack lists and, when the build cannot refresh packs in place,
     * asks the session to relaunch. The game process is killed and the launcher is reopened by
     * {@link MinecraftProcessRestarter}, then this runs and re-runs the normal launch so the
     * player lands back in the same instance with the new pack list — no manual relaunch.
     */
    private void maybeRelaunchForPendingRestart() {
        if (!org.chimeramc.client.core.minecraft.MinecraftSessionRestarter
                .consumePendingRelaunch(this)) return;
        if (versionManager == null) return;
        if (isFinishing() || isDestroyed()) return;
        GameVersion version = versionManager.getSelectedVersion();
        if (version == null) return;
        Intent launchIntent = createMinecraftLaunchIntent();
        minecraftLauncher.launch(launchIntent, version, new MinecraftLauncher.LaunchCallback() {
            @Override
            public void onLaunchStarted() {
            }

            @Override
            public void onLaunchFailed(Throwable e) {
                runOnUiThread(() -> {
                    // Same classifier as the dialog path: the cause is named, then the raw text.
                    LaunchErrorDiagnosis.Cause cause = LaunchErrorDiagnosis.classify(e);
                    String detail = e != null ? e.getMessage() : null;
                    String message = getString(LaunchErrorDiagnosis.messageRes(cause))
                            + (detail != null ? "\n\n" + detail : "");
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleVersionDependentIntent() {
        if (versionManager == null || fileHandler == null) return;
        if (handleInstanceShortcutLaunch()) return;
        if (!forwardIncomingMinecraftResourceToRunningGame()) {
            checkResourcepack();
            handleIncomingFiles();
        }
        handleMinecraftUriLaunch();
    }

    private boolean handleInstanceShortcutLaunch() {
        Intent intent = getIntent();
        if (intent == null || !InstanceShortcutManager.ACTION_LAUNCH_INSTANCE.equals(intent.getAction())) {
            return false;
        }

        String type = intent.getStringExtra(InstanceShortcutManager.EXTRA_INSTANCE_TYPE);
        String key = intent.getStringExtra(InstanceShortcutManager.EXTRA_INSTANCE_KEY);
        intent.setAction(null);
        intent.removeExtra(InstanceShortcutManager.EXTRA_INSTANCE_TYPE);
        intent.removeExtra(InstanceShortcutManager.EXTRA_INSTANCE_KEY);
        setIntent(intent);

        GameVersion target = findShortcutVersion(type, key);
        if (target == null) {
            Toast.makeText(this, R.string.instance_shortcut_missing, Toast.LENGTH_LONG).show();
            return true;
        }

        versionManager.selectVersion(target);
        setTextMinecraftVersion();
        updateViewModelVersion();
        if (viewModel != null) viewModel.refreshMods();
        binding.getRoot().post(this::performActualLaunch);
        return true;
    }

    private GameVersion findShortcutVersion(String type, String key) {
        if (key == null || key.isEmpty()) return null;
        if (InstanceShortcutManager.TYPE_INSTALLED.equals(type)) {
            for (GameVersion candidate : versionManager.getInstalledVersions()) {
                if (key.equals(candidate.packageName)) return candidate;
            }
            return null;
        }
        if (InstanceShortcutManager.TYPE_CUSTOM.equals(type)) {
            for (GameVersion candidate : versionManager.getCustomVersions()) {
                if (key.equals(candidate.directoryName)) return candidate;
            }
        }
        return null;
    }

    private void initModsSection() {
        if (versionManager == null || viewModel == null) return;

        // The quick_mods_card is the single way into the mods screen. The old "Active Mods"
        // heading with a Manage button and a list of enabled mod names was a second, weaker
        // entry point to the same destination, so it is gone; only the per-instance count on
        // the hero card remains.
        viewModel.getModsLiveData().observe(this, this::updateModsUI);
    }

    private void updateViewModelVersion() {
        if (viewModel == null) return;
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        if (selectedVersion != null) {
            viewModel.setCurrentVersion(selectedVersion);
        }
    }

    private void checkResourcepack() {
        new ResourcepackHandler(
                this, minecraftLauncher, resourcepackExecutor
        ).checkIntentForResourcepack();
    }

    private void repairNeededVersions() {
        GameVersion selectedVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (selectedVersion != null && selectedVersion.needsRepair) {
            VersionManager.attemptRepairLibs(this, selectedVersion);
        }
    }

    private void requestBasicPermissions() {
        requestStoragePermissionForMigration(() -> {
            if (storageMigrationManager != null) {
                startStorageMigrationService();
            }
        });
    }

    private void requestStoragePermissionForMigration(Runnable onGranted) {
        permissionsHandler.requestPermission(PermissionsHandler.PermissionType.STORAGE, new PermissionsHandler.PermissionResultCallback() {
            @Override
            public void onPermissionGranted(PermissionsHandler.PermissionType type) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    if (onGranted != null) onGranted.run();
                }
            }

            @Override
            public void onPermissionDenied(PermissionsHandler.PermissionType type, boolean permanentlyDenied) {
                if (type == PermissionsHandler.PermissionType.STORAGE) {
                    Toast.makeText(MainActivity.this, R.string.storage_migration_permission_denied, Toast.LENGTH_LONG).show();
                    showBlockingMigrationRetryDialog(
                            getString(R.string.storage_migration_failed_title),
                            getString(R.string.storage_migration_permission_denied)
                    );
                }
            }
        });
    }

    private void showStorageMigrationPromptIfNeeded() {
        if (postMigrationInitialized || migrationPromptShown || migrationPromptCheckInFlight || storageMigrationManager == null || isFinishing() || isDestroyed()) return;
        if (StorageMigrationService.isMigrationRunning(this)) {
            resumeStorageMigrationService();
            return;
        }
        migrationPromptCheckInFlight = true;
        // onDestroy shuts this executor down, and the check can be requested while the screen is
        // going away. Submitting then throws on the UI thread. The gate flag is cleared so a later
        // resume can retry instead of being blocked from ever offering the migration.
        try {
            storageMigrationExecutor.execute(() -> {
                boolean shouldOfferMigration = false;
                try {
                    shouldOfferMigration = storageMigrationManager.shouldOfferMigration();
                } catch (Exception ignored) {
                    AppLog.warnOnce("CaramelClient", "MainActivity:1042", "Ignored a failure at MainActivity:1042", ignored);
                }
                boolean finalShouldOfferMigration = shouldOfferMigration;
                runOnUiThread(() -> {
                    migrationPromptCheckInFlight = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (!finalShouldOfferMigration) {
                        initializeAfterMigrationGate();
                        return;
                    }
                    if (migrationPromptShown || storageMigrationManager == null) return;
                    showStorageMigrationPromptDialog();
                });
            });
        } catch (RejectedExecutionException rejected) {
            migrationPromptCheckInFlight = false;
        }
    }

    private void showStorageMigrationPromptDialog() {
        migrationPromptShown = true;

        String message = getString(
                R.string.storage_migration_message,
                LauncherStorage.getTargetAppRootDisplayPath(this)
        );
        if (StorageMigrationService.readPersistedStatus(this)
                == StorageMigrationService.Status.INTERRUPTED) {
            message = message + "\n\n" + getString(R.string.storage_migration_interrupted_hint);
        }

        CustomAlertDialog dialog = new CustomAlertDialog(this)
                .setTitleText(getString(R.string.storage_migration_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.storage_migration_start), v -> {
                    if (storageMigrationManager.canReadLegacyRoot()) {
                        startStorageMigrationService();
                    } else {
                        requestBasicPermissions();
                    }
                })
                .setNegativeButton(getString(R.string.exit), v -> finishAffinity());
        dialog.setCancelable(false);
        dialog.show();
    }

    private void showPostEulaFlow() {
        if (postEulaFlowRunning) return;
        if (isInstanceShortcutIntent()) {
            showStorageMigrationPromptIfNeeded();
            return;
        }
        postEulaFlowRunning = true;
        ChangelogManager.showIfNeeded(this, () -> {
            postEulaFlowRunning = false;
            showStorageMigrationPromptIfNeeded();
        });
    }

    /**
     * Opens the first-run wizard as a self-sizing popup over the launcher.
     *
     * <p>The wizard used to be a full-screen destination the splash routed to, so it covered the
     * whole app with its own chrome. Presenting it over the launcher instead makes it read as a
     * popup, and it is opened here (once per process) rather than by the splash so the launcher is
     * already laid out behind it. {@link org.chimeramc.client.ui.activities.SetupWizardActivity#shouldShow}
     * still owns whether it is due.
     */
    private void showSetupWizardIfNeeded() {
        if (sWizardOverlayLaunched) return;
        if (isFinishing() || isDestroyed()) return;
        if (isInstanceShortcutIntent()) return;
        if (!org.chimeramc.client.ui.activities.SetupWizardActivity.shouldShow(this)) return;
        sWizardOverlayLaunched = true;
        startActivity(new Intent(this, org.chimeramc.client.ui.activities.SetupWizardActivity.class));
    }

    private boolean isInstanceShortcutIntent() {
        Intent intent = getIntent();
        return intent != null && InstanceShortcutManager.ACTION_LAUNCH_INSTANCE.equals(intent.getAction());
    }

    private void startStorageMigrationService() {
        if (isFinishing()) return;
        requestNotificationPermissionForMigration();
        showStorageMigrationDialog();
        StorageMigrationService.startMigration(this);
        bindStorageMigrationService();
    }

    private void resumeStorageMigrationService() {
        if (isFinishing()) return;
        requestNotificationPermissionForMigration();
        showStorageMigrationDialog();
        StorageMigrationService.startMigration(this);
        bindStorageMigrationService();
    }

    private void requestNotificationPermissionForMigration() {
        if (ApiLevels.belowTiramisu() || notificationPermissionLauncher == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void bindStorageMigrationService() {
        if (storageMigrationBound) return;
        if (!StorageMigrationService.isMigrationRunning(this)) return;
        Intent intent = new Intent(this, StorageMigrationService.class);
        bindService(intent, storageMigrationConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindStorageMigrationService() {
        if (!storageMigrationBound) return;
        if (storageMigrationService != null) {
            storageMigrationService.removeListener(storageMigrationListener);
        }
        unbindService(storageMigrationConnection);
        storageMigrationBound = false;
        storageMigrationService = null;
    }

    private void showStorageMigrationDialog() {
        if (isFinishing() || isDestroyed()) return;
        if (storageMigrationDialog != null && storageMigrationDialog.isShowing()) return;
        LibsRepairDialog dialog = new LibsRepairDialog(this);
        storageMigrationDialog = dialog;
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(shownDialog -> {
            if (storageMigrationDialog != dialog || isFinishing() || isDestroyed()) return;
            dialog.setTitleText(getString(R.string.storage_migration_progress_title));
            dialog.setSubtitleText(getString(R.string.storage_migration_progress_subtitle));
            dialog.setStatusText(getString(R.string.storage_migration_scanning));
            dialog.setEtaText(getString(R.string.storage_migration_eta_pending));
            dialog.setBackgroundHintText(getString(R.string.storage_migration_background_hint));
            dialog.setPauseButton("", null);
            dialog.setIndeterminate(true);
            dialog.updateProgress(0);
            if (lastMigrationState != null) {
                updateStorageMigrationDialog(lastMigrationState);
            }
        });
        dialog.show();
    }

    private void handleStorageMigrationState(StorageMigrationService.MigrationState state) {
        if (state == null || isFinishing()) return;
        lastMigrationState = state;
        if (state.isActive()) {
            showStorageMigrationDialog();
            updateStorageMigrationDialog(state);
            return;
        }
        if (state.isFinished()) {
            dismissStorageMigrationDialog(() -> showStorageMigrationResult(state));
            return;
        }
    }

    private void updateStorageMigrationDialog(StorageMigrationService.MigrationState state) {
        if (storageMigrationDialog == null || !storageMigrationDialog.isShowing()) return;
        if (state.status == StorageMigrationService.Status.SCANNING) {
            storageMigrationDialog.setIndeterminate(true);
            storageMigrationDialog.setStatusText(getString(R.string.storage_migration_scanning));
            storageMigrationDialog.setEtaText(getMigrationEtaText(state));
            storageMigrationDialog.updateProgress(0);
            return;
        }
        if (state.status != StorageMigrationService.Status.RUNNING) return;
        storageMigrationDialog.setIndeterminate(false);
        String progressDetail = getString(
                R.string.storage_migration_progress_detail,
                state.processedFiles,
                state.totalFiles,
                shortenMigrationPath(state.currentFile)
        );
        storageMigrationDialog.setStatusText(progressDetail);
        storageMigrationDialog.setEtaText(getMigrationEtaText(state));
        storageMigrationDialog.updateProgress(state.percent);
    }

    private void dismissStorageMigrationDialog(Runnable afterDismiss) {
        LibsRepairDialog dialog = storageMigrationDialog;
        storageMigrationDialog = null;
        if (dialog == null) {
            if (afterDismiss != null) afterDismiss.run();
            return;
        }
        dialog.setOnDismissAnimationEndListener(afterDismiss);
        if (dialog.isShowing()) {
            dialog.dismiss();
        } else if (afterDismiss != null) {
            afterDismiss.run();
        }
    }

    private void showStorageMigrationResult(StorageMigrationService.MigrationState state) {
        if (isFinishing()) return;
        if (state.status == StorageMigrationService.Status.COMPLETED) {
            boolean wasInitialized = postMigrationInitialized;
            initializeAfterMigrationGate();
            if (wasInitialized && versionManager != null) {
                versionManager.reload();
                setTextMinecraftVersion();
                updateViewModelVersion();
            }
            if (viewModel != null) viewModel.refreshMods();
            refreshContentCounts();
            new CustomAlertDialog(MainActivity.this)
                    .setTitleText(getString(R.string.storage_migration_completed_title))
                    .setMessage(getString(
                            R.string.storage_migration_completed_message,
                            state.totalFiles,
                            formatBytes(state.totalBytes),
                            state.skippedFiles
                    ))
                    .setPositiveButton(getString(R.string.confirm), null)
                    .show();
        } else if (state.status == StorageMigrationService.Status.PARTIAL) {
            showBlockingMigrationRetryDialog(
                    getString(R.string.storage_migration_partial_title),
                    getString(
                            R.string.storage_migration_partial_message,
                            state.failedFiles,
                            state.totalFiles
                    )
            );
        } else if (state.status == StorageMigrationService.Status.FAILED) {
            showBlockingMigrationRetryDialog(
                    getString(R.string.storage_migration_failed_title),
                    getString(R.string.storage_migration_failed_message, state.errorMessage)
            );
        } else if (state.status == StorageMigrationService.Status.INTERRUPTED) {
            String message = state.errorMessage != null && !state.errorMessage.isEmpty()
                    ? state.errorMessage
                    : getString(R.string.storage_migration_interrupted_hint);
            showBlockingMigrationRetryDialog(
                    getString(R.string.storage_migration_interrupted_title),
                    message
            );
        }
    }

    private void showBlockingMigrationRetryDialog(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        migrationPromptShown = false;
        CustomAlertDialog dialog = new CustomAlertDialog(MainActivity.this)
                .setTitleText(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.retry), v -> showStorageMigrationPromptIfNeeded())
                .setNegativeButton(getString(R.string.exit), v -> finishAffinity());
        dialog.setCancelable(false);
        dialog.show();
    }

    private String shortenMigrationPath(String path) {
        if (path == null || path.isEmpty()) return "";
        final int max = 48;
        return path.length() <= max ? path : "..." + path.substring(path.length() - max);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(java.util.Locale.getDefault(), "%.1f MB", mb);
        return String.format(java.util.Locale.getDefault(), "%.1f GB", mb / 1024.0);
    }

    private String getMigrationEtaText(StorageMigrationService.MigrationState state) {
        if (state.estimatedRemainingMillis < 0L || state.estimatedCompletionAtMillis <= 0L) {
            return getString(R.string.storage_migration_eta_pending);
        }
        String remaining = formatMigrationDuration(state.estimatedRemainingMillis);
        String completionTime = DateFormat.getTimeInstance(DateFormat.SHORT, java.util.Locale.getDefault())
                .format(new Date(state.estimatedCompletionAtMillis));
        return getString(R.string.storage_migration_eta_detail, remaining, completionTime);
    }

    private String formatMigrationDuration(long millis) {
        long seconds = Math.max(1L, Math.round(millis / 1000.0d));
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        if (hours > 0L) {
            return getString(R.string.storage_migration_duration_hours_minutes, hours, minutes);
        }
        if (minutes > 0L) {
            return getString(R.string.storage_migration_duration_minutes_seconds, minutes, remainingSeconds);
        }
        return getString(R.string.storage_migration_duration_seconds, remainingSeconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyPersonalization();
        boolean anims = new PersonalizationManager(this).isShowAnimations();
        if (!anims) {
            stopHeroCardPulse();
        } else if (!pulseRunning) {
            startHeroCardPulse();
        }
        refreshAccountHeaderUI();
        if (StorageMigrationService.isMigrationRunning(this)) {
            resumeStorageMigrationService();
            return;
        }
        if (!postMigrationInitialized) {
            showPostEulaFlow();
            return;
        }
        if (versionManager != null) {
            setTextMinecraftVersion();
            viewModel.refreshMods();
            refreshContentCounts();
        }
        if (binding != null && versionManager != null) {
            resetLaunchButton();
        }
        // The equipped cosmetics can change on the Cosmetics screen, so re-apply them and restart
        // the hero preview's animation on every resume.
        bindLaunchPreviewCosmetics();
        if (launchPreview != null) launchPreview.startPreview();
        refreshLaunchToggles();
        announcePendingLaunchFailure();
    }

    /**
     * Reports a launch that died inside the game activity.
     *
     * <p>That failure cannot show a dialog where it happens: {@code MinecraftActivity} tears the
     * launcher process down to restart it, so the Activity that would own the dialog is already
     * gone. The cause is stored instead and named here, on the screen the user lands back on — the
     * step that was missing, and the reason a failed launch used to look like the app simply
     * closing.
     *
     * <p>Runs at the end of {@code onResume}, after the EULA and migration gates have returned, so
     * the failure is never shown over a first-run or migration prompt. It consumes the stored
     * report, so it is announced exactly once; the file is also mirrored into the Diagnostics
     * report for as long as it exists.
     */
    private void announcePendingLaunchFailure() {
        if (isFinishing() || isDestroyed()) return;
        org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure.Report report =
                org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure
                        .consumePending(this);
        if (report == null) return;
        org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis.Cause cause = report.cause();
        String detail = org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure
                .detailOf(report);
        StringBuilder message = new StringBuilder(
                getString(org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis
                        .messageRes(cause)));
        if (!detail.isEmpty()) {
            message.append("\n\n").append(getString(R.string.launch_error_cause_label)).append(' ')
                    .append(org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis
                            .label(cause))
                    .append('\n').append(detail);
        }
        new CustomAlertDialog(this)
                .setTitleText(getString(org.chimeramc.client.launcher.core.minecraft
                        .LaunchErrorDiagnosis.titleRes(cause)))
                .setMessage(message.toString())
                .setPositiveButton(getString(R.string.diagnostics_title),
                        v -> startActivity(new Intent(this,
                                org.chimeramc.client.launcher.ui.activities.DiagnosticsActivity.class)))
                .setNegativeButton(getString(R.string.dialog_positive_ok), null)
                .show();
    }

    private void applyPersonalization() {
        PersonalizationManager pm = new PersonalizationManager(this);
        
        // Apply Animation Speed
        float animSpeed = pm.getAnimationSpeed();
        DynamicAnim.setGlobalSpeedMultiplier(animSpeed);
        
        // Apply UI Transparency
        int transparency = pm.getUiTransparency();
        if (binding != null && binding.getRoot() != null) {
            float alpha = transparency / 100f;
            binding.getRoot().setAlpha(alpha);
        }
        
        // Apply Card Rounding
        int rounding = pm.getCardRounding();
        applyCardRounding(rounding);
        
        // Apply Icon Size
        int iconSize = pm.getIconSize();
        applyIconSize(iconSize);
        
        // Apply Blur Intensity
        int blur = pm.getBlurIntensity();
        applyBlurIntensity(blur);
        
        // Apply Glow Effects
        boolean glow = pm.isEnableGlowEffects();
        applyGlowEffects(glow);

        // The motion scheme is applied by BaseActivity.applyMotionScheme for every screen, so
        // this screen must not set it a second time (one owner).
    }
    
    private void applyCardRounding(int roundingDp) {
        if (binding == null) return;
        float density = getResources().getDisplayMetrics().density;
        float radiusPx = roundingDp * density;
        
        // The launch face is a state-list drawable, so there is no single GradientDrawable to
        // retune; its 20dp radius comes from @dimen/radius_split_launch and is deliberately fixed,
        // because the corner-radius preference is a card/shape preference and the pack's launch
        // control is its own shape. Rounding it here would also break the flush seam with the
        // chevron segment, which shares that radius on its trailing edge only.
        
        // Apply to select version button
        if (binding.launchDock.selectVersionButton.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) binding.launchDock.selectVersionButton.getBackground()).setCornerRadius(radiusPx);
        }
    }
    
    private void applyIconSize(int sizePercent) {
        // The preference is a real scale on every fixed-size icon in the tree (see IconSizeApplier
        // for why the base size is cached rather than read from the live dimension).
        if (binding == null || binding.getRoot() == null) return;
        IconSizeApplier.apply(binding.getRoot(), sizePercent);
    }
    
    private void applyBlurIntensity(int intensity) {
        // Blur intensity is not a blur: this method dims the dashboard. Painting a black
        // scrim over the whole root also darkens the wallpaper image that the user picked,
        // so the scrim is only applied while a background image is actually in use. Without
        // one there is nothing behind the cards to blur, and the scrim would just make the
        // dashboard needlessly dark.
        if (binding == null || binding.getRoot() == null) return;
        PersonalizationManager pm = new PersonalizationManager(this);
        if (!pm.hasBackgroundImage()) {
            // Leave the layout's own background alone rather than painting a scrim
            // over a dashboard that has nothing behind it.
            return;
        }
        float dimAmount = intensity / 200f; // 0.0 to 0.5
        binding.getRoot().setBackgroundColor(Color.argb((int) (dimAmount * 255), 0, 0, 0));
    }

    private void applyGlowEffects(boolean enabled) {
        float glowElevation = enabled ? dp(8f) : dp(0f);
        float cardElevation = enabled ? dp(3f) : dp(0f);

        if (binding != null) {
            if (binding.launchDock.launchButton != null) binding.launchDock.launchButton.setElevation(glowElevation);
            if (binding.launchDock.getRoot() != null) binding.launchDock.getRoot().setElevation(cardElevation);
        }

        int[] cardIds = {
                R.id.launch_hero_compose,
                R.id.launch_toggles_compose
        };
        for (int id : cardIds) {
            View card = findViewById(id);
            if (card != null) {
                card.setElevation(enabled ? dp(4f) : dp(0f));
                if (enabled && card.getBackground() instanceof GradientDrawable && card.getId() == R.id.launch_hero_compose) {
                    GradientDrawable bg = (GradientDrawable) card.getBackground().mutate();
                    int accent = new PersonalizationManager(this).getAccentColor();
                    bg.setStroke((int) dp(1.5f), accent != 0 ? accent : getColor(R.color.grad_pulse_glow));
                }
            }
        }
    }
    
    private void startHeroCardPulse() {
        View heroCard = findViewById(R.id.launch_hero_compose);
        if (heroCard == null) return;
        PersonalizationManager pm = new PersonalizationManager(this);
        if (!pm.isShowAnimations()) return;
        if (pulseRunning) return;
        // Cancel any stale per-card touches (spring shenanigans) so the pulse owns the card.
        heroCard.animate().cancel();
        pulseRunning = true;
        int glowColor = pm.isDarkMode(this)
                ? getColor(R.color.grad_pulse_glow)
                : pm.getAccentColor() != 0 ? pm.getAccentColor() : getColor(R.color.grad_pulse_glow);
        pulseRunnable = new Runnable() {
            @Override
            public void run() {
                if (!pulseRunning) return;
                // A gentle lift-and-settle: elevation and a hair of scale, never alpha. Dimming the
                // card faded its own text with it, which read as a flicker rather than a pulse.
                heroCard.animate().cancel();
                heroCard.animate()
                        .translationZ(dp(6f))
                        .scaleX(1.012f)
                        .scaleY(1.012f)
                        .setDuration(700L)
                        .setInterpolator(DynamicAnim.getDefaultInterpolator())
                        .start();
                heroCard.postDelayed(() -> {
                    if (!pulseRunning) return;
                    heroCard.animate()
                            .translationZ(dp(0f))
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(900L)
                            .setInterpolator(DynamicAnim.getDefaultInterpolator())
                            .withLayer()
                            .start();
                }, 700L);
                uiHandler.postDelayed(this, 2400L);
            }
        };
        // Straightforward glow ring via a highlight drawable when glow effects are on.
        if (pm.isEnableGlowEffects() && heroCard.getBackground() instanceof GradientDrawable) {
            GradientDrawable base = (GradientDrawable) heroCard.getBackground().mutate();
            base.setStroke((int) dp(1.5f), glowColor);
        }
        uiHandler.post(pulseRunnable);
    }

    private void stopHeroCardPulse() {
        pulseRunning = false;
        if (pulseRunnable != null) {
            uiHandler.removeCallbacks(pulseRunnable);
            pulseRunnable = null;
        }
        View heroCard = findViewById(R.id.launch_hero_compose);
        if (heroCard != null) {
            heroCard.animate().cancel();
            heroCard.animate().translationZ(0f).alpha(1f)
                    .scaleX(1f).scaleY(1f).setDuration(200L).start();
        }
    }

    /**
     * Smoothly advances the launch progress bar (with a subtle glow on the thumb),
     * instead of snapping between values. The bar is revealed on the first update
     * and hidden again once any file import finishes.
     */
    private void animateProgressTo(int target) {
        if (binding == null || binding.launchDock.progressLoader == null) return;
        if (activeProgressAnimator != null) {
            activeProgressAnimator.cancel();
        }
        if (binding.launchDock.progressLoader.getVisibility() != View.VISIBLE) {
            binding.launchDock.progressLoader.setIndeterminate(false);
            binding.launchDock.progressLoader.setProgress(0);
            binding.launchDock.progressLoader.setVisibility(View.VISIBLE);
        }
        PersonalizationManager pm = new PersonalizationManager(this);
        int from = binding.launchDock.progressLoader.getProgress();
        int duration = pm.isShowAnimations() ? 350 : 0;
        if (duration == 0) {
            binding.launchDock.progressLoader.setProgress(target);
            return;
        }
        activeProgressAnimator = ValueAnimator.ofInt(from, target).setDuration(duration);
        activeProgressAnimator.setInterpolator(DynamicAnim.getDefaultInterpolator());
        activeProgressAnimator.addUpdateListener(animator ->
                binding.launchDock.progressLoader.setProgress((Integer) animator.getAnimatedValue()));
        activeProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeProgressAnimator = null;
            }
        });
        activeProgressAnimator.start();
    }

    private void hideProgressLoader() {
        if (binding == null || binding.launchDock.progressLoader == null) return;
        if (activeProgressAnimator != null) {
            activeProgressAnimator.cancel();
            activeProgressAnimator = null;
        }
        binding.launchDock.progressLoader.setProgress(0);
        binding.launchDock.progressLoader.setVisibility(View.GONE);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onStop() {
        stopHeroCardPulse();
        stopLaunchShimmer();
        if (launchPreview != null) launchPreview.stopPreview();
        if (activeProgressAnimator != null) {
            activeProgressAnimator.cancel();
            activeProgressAnimator = null;
        }
        unbindStorageMigrationService();
        super.onStop();
    }






    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionsHandler != null) {
            permissionsHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeIntentLaunch"})
    private void initListeners() {
        setLaunchControlEnabled(false);
        binding.launchDock.launchButton.setOnClickListener(v -> launchGame());
        DynamicAnim.applyPressScale(binding.launchDock.launchButton);
        // The split button's chevron is the other half: it opens the instance/version picker, so
        // tapping the wrong half never starts a session.
        binding.launchDock.launchChevron.setOnClickListener(v -> showVersionSelectDialog());
        DynamicAnim.applyPressScale(binding.launchDock.launchChevron);
        binding.launchDock.selectVersionButton.setOnClickListener(v -> showVersionSelectDialog());
        DynamicAnim.applyPressScale(binding.launchDock.selectVersionButton);

        // Setup Quick Actions Grid
        setupQuickActions();

        // Setup Launch-tab quick toggles (Mods / Voice)
        setupLaunchToggles();

        // Setup the docked launch controls' shimmer feedback
        setupDockFeedback();

        // Setup Account Header
        setupAccountHeader();

        // Setup Launch-tab section headers
        setupSectionHeaders();

        FeatureSettings.init(getApplicationContext());
        showRandomTip();
        setupAnnouncementCard();
        setupLaunchChangelog();
        // One mod-update notification a day, never a prompt per mod. The scheduler applies
        // the feature switch and the throttle, and does nothing at all when this build has no feed.
        org.chimeramc.client.core.mods.update.ModUpdateScheduler.requestCheck(this);
    }

    /**
     * Sets up the release-notes card: parses the bundled changelog and pushes it into Compose.
     *
     * <p>The parse runs on the existing launch-stats executor rather than the UI thread, because the
     * card is not needed for the first frame and the JSON is a couple of kilobytes. A build with no
     * release notes leaves the card hidden rather than opening an empty screen.
     */
    private void setupLaunchChangelog() {
        changelogCompose = findViewById(R.id.launch_changelog_compose);
        if (changelogCompose == null) return;
        changelogState = org.chimeramc.client.ui.compose.launch.LaunchChangelogHost.state(
                org.chimeramc.client.ui.compose.launch.LaunchChangelogStateBuilder.build(
                        org.chimeramc.client.core.changelog.LaunchChangelog.empty(),
                        BuildConfig.VERSION_NAME));
        changelogOpen = org.chimeramc.client.ui.compose.launch.LaunchChangelogHost.openState(false);
        org.chimeramc.client.ui.compose.launch.LaunchChangelogHost.setContent(
                changelogCompose, changelogState, changelogOpen);
        SafeExecutor.submit(launchStatsExecutor, () -> {
            org.chimeramc.client.core.changelog.LaunchChangelog parsed =
                    org.chimeramc.client.core.changelog.LaunchChangelog.parse(readBundledChangelog());
            androidx.compose.runtime.MutableState<org.chimeramc.client.ui.compose.launch.LaunchChangelogState> state = changelogState;
            if (state == null) return;
            state.setValue(org.chimeramc.client.ui.compose.launch.LaunchChangelogStateBuilder.build(
                    parsed, BuildConfig.VERSION_NAME));
        });
    }

    /** Reads the bundled release-notes asset; a missing or unreadable file reads as empty. */
    private String readBundledChangelog() {
        try (java.io.InputStream in = getAssets().open("launcher/changelog.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Throwable t) {
            AppLog.warnOnce("CaramelClient", "MainActivity:changelog",
                    "Reading the bundled release notes failed", t);
            return null;
        }
    }

    /**
     * Shows the announcement card when the feed yields one that applies to this build.
     *
     * <p>The card is hidden by default and only made visible for a matching, undismissed card, so a
     * failed fetch leaves the Launch tab exactly as it was. Dismissing writes the id and hides the
     * card; the cached feed means the choice survives a restart.
     */
    private void setupAnnouncementCard() {
        if (binding.launchAnnouncement == null) return;
        View card = binding.launchAnnouncement.announcementCard;
        TextView title = binding.launchAnnouncement.announcementTitle;
        TextView body = binding.launchAnnouncement.announcementBody;
        TextView link = binding.launchAnnouncement.announcementLink;
        View dismiss = binding.launchAnnouncement.announcementDismiss;

        announcementClient = new org.chimeramc.client.core.announce.AnnouncementClient();
        announcementClient.load(this, BuildConfig.VERSION_CODE, announcement -> {
            if (announcement == null) {
                card.setVisibility(View.GONE);
                return;
            }
            title.setText(announcement.title);
            body.setText(announcement.body);
            if (announcement.hasLink() && link != null) {
                link.setVisibility(View.VISIBLE);
                link.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(announcement.url)));
                    } catch (Throwable ignored) {
                        AppLog.warnOnce("CaramelClient", "MainActivity:1721", "Ignored a failure at MainActivity:1721", ignored);
                    }
                });
            } else if (link != null) {
                link.setVisibility(View.GONE);
            }
            if (dismiss != null) {
                dismiss.setOnClickListener(v -> {
                    org.chimeramc.client.core.announce.AnnouncementStore.dismiss(this, announcement.id);
                    card.setVisibility(View.GONE);
                });
            }
            card.setVisibility(View.VISIBLE);
            DynamicAnim.applyPressScale(card);
        });
    }

    private void setupQuickActions() {
        // The hero is a Compose screen now and owns its own click (the host passes launchGame as
        // onHeroClick), so there is nothing to wire here beyond the sections below.

        // Refresh hero card stats
        refreshLastPlayedCard();
        startHeroCardPulse();

        // Quick Launch Card was removed: the hero card and PLAY MINECRAFT already launch, and
        // version selection now lives on the dock's Select Version button.

        // Quick Mods Card and Quick Content Card were removed with the old quick-action grid:
        // the Launch tab now uses purpose-built quick toggles (Mods, Voice)
        // and the content rows below.
    }

    /**
     * Wires the Launch tab's quick-toggle cards. Each card is a shortcut into a feature and shows
     * that feature's live state on its own status line, refreshed whenever the tab resumes.
     */
    private void setupLaunchToggles() {
        setupLaunchHero();
        setupLaunchToggleRow();
        refreshLaunchToggles();
    }

    /** Hosts the Compose quick-toggle row and keeps its data in step with the live statuses. */
    private void setupLaunchToggleRow() {
        View host = findViewById(R.id.launch_toggles_compose);
        if (!(host instanceof androidx.compose.ui.platform.ComposeView)) return;
        toggleCompose = (androidx.compose.ui.platform.ComposeView) host;
        toggleState = org.chimeramc.client.ui.compose.launch.LaunchTogglesHost.state(
                buildToggleRow());
        toggleCompose.setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        org.chimeramc.client.ui.compose.launch.LaunchTogglesHost.setContent(
                toggleCompose,
                toggleState,
                kind -> {
                    switch (kind) {
                        case MODS:
                            openModsFullscreen();
                            break;
                        case VOICE:
                            startActivity(new Intent(this,
                                    org.chimeramc.client.ui.activities.VoiceChatActivity.class));
                            break;
                        case REPLAY:
                            LibraryActivity.open(this, LibraryActivity.TAB_REPLAY);
                            break;
                        default:
                            break;
                    }
                });
    }

    /** The row's data: guest mode hides the Mods and Voice cards. */
    private java.util.List<org.chimeramc.client.ui.compose.launch.LaunchToggleState.Toggle>
            buildToggleRow() {
        boolean guest = org.chimeramc.client.core.privacy.PrivacyModes.current(this).isGuestMode();
        return org.chimeramc.client.ui.compose.launch.LaunchToggleState.visible(
                guest,
                getString(R.string.launch_toggle_mods), modsStatusText,
                getString(R.string.launch_toggle_voice), voiceStatusText,
                getString(R.string.launch_toggle_replay), replayStatusText);
    }

    /** Rebuilds the toggle row from the live statuses and recomposes. */
    private void refreshToggleRow() {
        if (toggleState == null) return;
        toggleState.setValue(buildToggleRow());
    }

    /**
     * Wires the docked launch controls' feedback: the sweeping shimmer that runs while the launcher
     * is still preparing (assets/native bridges loading), and the mirror of the Play button's
     * enabled state onto the dock.
     */
    private void setupDockFeedback() {
        launchShimmer = findViewById(R.id.launch_button_shimmer);
        View play = binding != null ? binding.launchDock.launchButton : null;
        if (play == null) return;
        // The button starts disabled until the version manager is ready; start the shimmer now and
        // let resetLaunchButton() stop it, so the control never reads as frozen on a cold start.
        startLaunchShimmer();
    }

    private void startLaunchShimmer() {
        if (launchShimmer == null) return;
        PersonalizationManager pm = new PersonalizationManager(this);
        if (!pm.isShowAnimations()) {
            launchShimmer.setVisibility(View.GONE);
            return;
        }
        if (shimmerRunning) return;
        shimmerRunning = true;
        launchShimmer.setVisibility(View.VISIBLE);
        launchShimmer.setTranslationX(-launchShimmer.getWidth());
        shimmerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!shimmerRunning || launchShimmer == null) return;
                float width = launchShimmer.getWidth() > 0
                        ? launchShimmer.getWidth()
                        : getResources().getDisplayMetrics().widthPixels;
                launchShimmer.setTranslationX(-width);
                launchShimmer.animate()
                        .translationX(width)
                        .setDuration(1100L)
                        .setInterpolator(new android.view.animation.LinearInterpolator())
                        .withEndAction(() -> {
                            if (!shimmerRunning) return;
                            uiHandler.postDelayed(shimmerRunnable, 350L);
                        })
                        .start();
            }
        };
        uiHandler.post(shimmerRunnable);
    }

    private void stopLaunchShimmer() {
        shimmerRunning = false;
        if (shimmerRunnable != null) {
            uiHandler.removeCallbacks(shimmerRunnable);
            shimmerRunnable = null;
        }
        if (launchShimmer != null) {
            launchShimmer.animate().cancel();
            launchShimmer.setVisibility(View.GONE);
        }
    }

    /**
     * Refreshes the quick-toggle status lines from each feature's own state: the selected version's
     * mods directory for installed mods, and the voice module's running state and route. Nothing
     * here is a second source of truth.
     */
    private void refreshLaunchToggles() {
        // Voice is in-memory state, so it is refreshed straight away.
        org.chimeramc.client.core.voice.VoiceChatModule module =
                org.chimeramc.client.core.voice.VoiceChatModule.peek();
        boolean on = module != null && module.isRunning();
        voiceStatusText = on
                ? getString(R.string.launch_voice_status_on, getVoiceRouteLabel())
                : getString(R.string.launch_voice_status_off);
        refreshToggleRow();

        // The other two read the disk: the mods count lists a directory and the Replay count scans
        // the replays folder and asks MediaMetadataRetriever for every clip's duration. On the UI
        // thread that ran on every onResume, so returning from any screen blocked the frame while
        // the video metadata was parsed. Gather both on a worker and paint when they arrive.
        if (toggleState == null) return;
        // onResume can run against an executor that onDestroy has already shut down, and an
        // escaping RejectedExecutionException would take the Launch tab down with it. The statuses
        // then simply keep their last painted value, which is the right degradation for a
        // decorative line.
        try {
            launchStatsExecutor.execute(() -> {
                final int modCount = countInstalledMods();
                final int replayCount = countReplayClips();
                uiHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    modsStatusText = modCount > 0
                            ? getResources().getQuantityString(R.plurals.launch_mods_status_ready, modCount, modCount)
                            : getString(R.string.launch_mods_status_none);
                    replayStatusText = replayCount > 0
                            ? getResources().getQuantityString(R.plurals.launch_replay_status_count, replayCount, replayCount)
                            : getString(R.string.launch_replay_status_none);
                    refreshToggleRow();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            AppLog.warnOnce("CaramelClient", "MainActivity:1904", "Ignored a failure at MainActivity:1904", ignored);
        }
    }

    /**
     * Applies the player's equipped cosmetics to the Launch-tab hero preview, using the same
     * {@code CosmeticStore} the Cosmetics panel writes, so the hero shows exactly what the game
     * will render (or the Optifine default cape when Optifine Mode is on).
     */
    private void bindLaunchPreviewCosmetics() {
        if (launchPreview == null) return;
        try {
            org.chimeramc.client.core.cosmetics.CosmeticStore store =
                    new org.chimeramc.client.core.cosmetics.CosmeticStore(this);
            launchPreview.setCape(store.getEquippedCapeForDisplay());
            launchPreview.setAccessory(store.getEquippedAccessory());
            launchPreview.setPet(store.getEquippedPet());
            // Re-resolve the skin so the hero shows the player's real Minecraft skin (read from the
            // game through the engine hook when available) rather than a stale or generic one.
            launchPreview.refreshSkin();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "MainActivity:1925", "Ignored a failure at MainActivity:1925", ignored);
        }
    }

    private int countInstalledMods() {
        try {
            GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;
            java.io.File modsDir = version != null ? version.modsDir : null;
            if (modsDir == null) return 0;
            java.io.File[] children = modsDir.listFiles();
            return children == null ? 0 : children.length;
        } catch (Throwable t) {
            return 0;
        }
    }

    private int countReplayClips() {
        try {
            return new org.chimeramc.client.core.replay.ReplayClipRepository(this).scan().size();
        } catch (Throwable t) {
            return 0;
        }
    }

    private String getVoiceRouteLabel() {
        try {
            InbuiltModManager manager = InbuiltModManager.getInstance(this);
            String relay = manager.getVoiceRelayAddress();
            return (relay != null && !relay.isEmpty()) ? "Relay" : "LAN";
        } catch (Throwable t) {
            return "LAN";
        }
    }

    private void setupAccountHeader() {
        Button headerSignInButton = findViewById(R.id.header_sign_in_button);
        if (headerSignInButton != null) {
            headerSignInButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, MsftLoginActivity.class);
                accountLoginLauncher.launch(intent);
            });
            DynamicAnim.applyPressScale(headerSignInButton);
        }

        TextView headerAccountName = findViewById(R.id.header_account_name);
        com.microsoft.xbox.idp.toolkit.CircleImageView headerAvatar = findViewById(R.id.header_account_avatar);
        
        // Update header account info when account changes
        refreshAccountHeaderUI();
    }

    private void setupSectionHeaders() {
        bindSectionHeader(R.id.section_quick, R.string.quick_actions_title, R.drawable.ic_nav_launch, 0);
        View contentAction = bindSectionHeader(R.id.section_content,
                R.string.home_content_library, R.drawable.ic_content, R.string.view_all);
        if (contentAction != null) {
            contentAction.setOnClickListener(v -> openContentManagement());
            DynamicAnim.applyPressScale(contentAction);
        }
        bindSectionHeader(R.id.section_more, R.string.home_more, R.drawable.ic_modules, 0);
    }

    /**
     * Fills one included section header. The header layout carries a shared action view, so a
     * section with no action leaves it {@code GONE} rather than shipping a second near-identical
     * header layout that would drift from this one.
     *
     * @return the trailing action view when one was shown, so the caller can wire its click;
     *         {@code null} when the section has no action
     */
    private View bindSectionHeader(int includeId, int titleRes, int actionRes) {
        return bindSectionHeader(includeId, titleRes, 0, actionRes);
    }

    private View bindSectionHeader(int includeId, int titleRes, int iconRes, int actionRes) {
        View include = findViewById(includeId);
        if (include == null) return null;
        TextView title = include.findViewById(R.id.section_title);
        if (title != null) title.setText(titleRes);
        ImageView icon = include.findViewById(R.id.section_icon);
        if (icon != null) {
            if (iconRes != 0) {
                icon.setImageResource(iconRes);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
        }
        TextView action = include.findViewById(R.id.section_action);
        if (action == null) return null;
        if (actionRes == 0) {
            action.setVisibility(View.GONE);
            return null;
        }
        action.setText(actionRes);
        action.setVisibility(View.VISIBLE);
        return action;
    }

    /**
     * Shows the tip carousel, or hides it once the launch budget is spent.
     *
     * <p>The retirement and the no-repeat rotation are {@link org.chimeramc.client.core.stats
     * .TipsCarousel}'s rules; this method only paints the result and owns the timer. The panel is
     * hidden rather than left showing a stale tip, because "no tips any more" is a deliberate
     * state, not an empty rotation.
     */
    private void showRandomTip() {
        String[] tips = getResources().getStringArray(R.array.launcher_tips);
        if (binding.tipText == null) return;

        int launches = org.chimeramc.client.core.stats.LaunchStatsRecorder
                .load(this).launches();
        if (tips.length == 0 || !org.chimeramc.client.core.stats.TipsCarousel.shouldShow(launches)) {
            binding.tipText.setVisibility(android.view.View.GONE);
            return;
        }

        final java.util.Random random = new java.util.Random();
        tipIndex = org.chimeramc.client.core.stats.TipsCarousel.nextIndex(
                tips.length, tipIndex, random.nextInt(Integer.MAX_VALUE));
        binding.tipText.setVisibility(android.view.View.VISIBLE);
        binding.tipText.setText(tips[tipIndex]);

        // The tip rotation is a self-reposting tick. It runs through the activity's scope so
        // onDestroy removes the pending post instead of firing into a destroyed Activity
        //.
        if (tipScope.isClosed()) return;
        tipScope.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (binding == null || binding.tipText == null) return;
                final int next = org.chimeramc.client.core.stats.TipsCarousel.nextIndex(
                        tips.length, tipIndex, random.nextInt(Integer.MAX_VALUE));
                binding.tipText.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                    if (binding == null || binding.tipText == null) return;
                    tipIndex = next;
                    binding.tipText.setText(tips[tipIndex]);
                    binding.tipText.animate().alpha(1f).setDuration(300).start();
                }).start();
                tipScope.postDelayed(this, TIP_ROTATE_MS);
            }
        }, TIP_ROTATE_MS);
    }

    private void initContentManagementSection() {
        worldsCountText = binding.contentWorldsCount;
        resourcePacksCountText = binding.contentResourcePacksCount;
        behaviorPacksCountText = binding.contentBehaviorPacksCount;

        contentManager = ContentManager.getInstance(this);
        contentManager.getWorldsLiveData().observe(this, worlds -> {
            if (worldsCountText != null)
                worldsCountText.setText(String.valueOf(worlds != null ? worlds.size() : 0));
        });
        contentManager.getResourcePacksLiveData().observe(this, packs -> {
            if (resourcePacksCountText != null)
                resourcePacksCountText.setText(String.valueOf(packs != null ? packs.size() : 0));
        });
        contentManager.getBehaviorPacksLiveData().observe(this, packs -> {
            if (behaviorPacksCountText != null)
                behaviorPacksCountText.setText(String.valueOf(packs != null ? packs.size() : 0));
        });

        binding.contentWorldsRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_WORLDS));
        binding.contentResourcePacksRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_RESOURCE_PACKS));
        binding.contentBehaviorPacksRow.setOnClickListener(v -> openContentList(ContentListActivity.TYPE_BEHAVIOR_PACKS));
        DynamicAnim.applyPressScale(binding.contentWorldsRow);
        DynamicAnim.applyPressScale(binding.contentResourcePacksRow);
        DynamicAnim.applyPressScale(binding.contentBehaviorPacksRow);

        refreshContentCounts();
    }

    private void refreshContentCounts() {
        if (versionManager == null || contentManager == null) return;
        GameVersion currentVersion = versionManager.getSelectedVersion();
        if (currentVersion == null) return;

        android.content.SharedPreferences cmPrefs = getSharedPreferences("content_management", MODE_PRIVATE);
        String savedType = cmPrefs.getString("storage_type", "INTERNAL");
        org.chimeramc.client.settings.FeatureSettings.StorageType storageType;
        try {
            storageType = org.chimeramc.client.settings.FeatureSettings.StorageType.valueOf(savedType);
        } catch (IllegalArgumentException e) {
            storageType = org.chimeramc.client.settings.FeatureSettings.StorageType.INTERNAL;
        }
        storageType = LauncherStorage.normalizeContentStorageType(
                storageType,
                currentVersion.versionIsolation
        );

        java.io.File baseDir = LauncherStorage.getContentGameDataDir(
                this,
                currentVersion.getStorageProfileId(),
                storageType
        );

        contentManager.setStorageDirectories(
                new java.io.File(baseDir, "minecraftWorlds"),
                new java.io.File(baseDir, "resource_packs"),
                new java.io.File(baseDir, "behavior_packs"),
                new java.io.File(baseDir, "skin_packs"),
                new java.io.File(baseDir, "Screenshots"),
                new java.io.File(baseDir, "minecraftpe"));
    }

    private void openContentList(int contentType) {
        GameVersion currentVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (currentVersion == null) {
            Toast.makeText(this, getString(R.string.not_found_version), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ContentListActivity.class);
        intent.putExtra(ContentListActivity.EXTRA_CONTENT_TYPE, contentType);
        startActivity(intent);
    }

    private static void setRowVisible(View view, boolean visible) {
        if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void initMiscellaneousSection() {
        binding.miscCurseforgeRow.setOnClickListener(v -> startActivity(new Intent(this, CurseForgeActivity.class)));
        binding.miscModHubRow.setOnClickListener(v -> startActivity(new Intent(this, ModHubActivity.class)));
        // Guest mode hides the mod-download rows; both are binding fields, so this adds
        // no new lookup.
        boolean guest = org.chimeramc.client.core.privacy.PrivacyModes.current(this).isGuestMode();
        setRowVisible(binding.miscModHubRow, !guest);
        setRowVisible(binding.miscCurseforgeRow, !guest);
        binding.miscAccountsRow.setOnClickListener(v -> startActivity(new Intent(this, AccountsActivity.class)));
        binding.miscQuickLaunchRow.setOnClickListener(v -> startActivity(new Intent(this, QuickLaunchActivity.class)));
        DynamicAnim.applyPressScale(binding.miscCurseforgeRow);
        DynamicAnim.applyPressScale(binding.miscModHubRow);
        DynamicAnim.applyPressScale(binding.miscAccountsRow);
        DynamicAnim.applyPressScale(binding.miscQuickLaunchRow);
        initLaunchQuickDock();
    }

    /**
     * The Launch tab's quick dock: Mods, Cosmetics and the radial wheel.
     *
     * <p>Each entry opens the destination the spec names. Mods and Cosmetics open their tabs;
     * the wheel entry asks the shared radial wheel to show, which is the same overlay the in-game
     * bind uses, so the dock cannot drift from the in-game menu. The version label carries the
     * real build version rather than a hardcoded string.
     */
    private void initLaunchQuickDock() {
        View dock = findViewById(R.id.launch_quickdock);
        if (dock == null) return;

        View mods = findViewById(R.id.quickdock_mods);
        if (mods != null) {
            mods.setOnClickListener(v -> startActivity(new Intent(this, ModsFullscreenActivity.class)));
            DynamicAnim.applyPressScale(mods);
            DynamicAnim.applyHoverBloom(mods);
            // Guest mode removes the Mods shortcut from the dock too.
            setRowVisible(mods, !org.chimeramc.client.core.privacy.PrivacyModes.current(this)
                    .isGuestMode());
        }
        View cosmetics = findViewById(R.id.quickdock_cosmetics);
        if (cosmetics != null) {
            cosmetics.setOnClickListener(v -> openProfileSheet());
            DynamicAnim.applyPressScale(cosmetics);
            DynamicAnim.applyHoverBloom(cosmetics);
        }
        View wheel = findViewById(R.id.quickdock_wheel);
        if (wheel != null) {
            wheel.setOnClickListener(v -> openModsFromHome());
            DynamicAnim.applyPressScale(wheel);
            DynamicAnim.applyHoverBloom(wheel);
        }
    }

    /** Opens the Customize screen on a specific tab (Skins or Controller). */
    private void openCustomizeTab(int tab) {
        Intent intent = new Intent(this, CustomizeActivity.class);
        intent.putExtra(CustomizeActivity.EXTRA_INITIAL_TAB, tab);
        startActivity(intent);
    }

    /**
     * Opens the profile sheet: who you are, your cosmetics and your skins (U2).
     *
     * <p>Cosmetics are a property of the player, not a destination, so they are reached from the
     * avatar and the dock's Cosmetics entry rather than from a top-level tab.
     */
    private void openProfileSheet() {
        try {
            new org.chimeramc.client.launcher.ui.dialogs.ProfileSheet(this).show();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "MainActivity:2221", "Ignored a failure at MainActivity:2221", ignored);
        }
    }

    /**
     * Opens the launcher's Mods screen.
     *
     * <p>This dock entry used to raise the radial wheel over the launcher. The wheel was removed
     * with the Java menu screen, and the launcher cannot host the native menu (that draws on the
     * game's GL context and needs a running session), so the entry opens the launcher's own Mods
     * screen — the same module list, as a normal screen.
     */
    private void openModsFromHome() {
        openModsFullscreen();
    }

    private void openModsFullscreen() {
        Intent intent = new Intent(this, ModsFullscreenActivity.class);
        startActivity(intent);
    }

    private void launchGame() {
        if (!isVersionManagerReady()) return;
        performActualLaunch();
    }

    /** Enables or disables both halves of the split launch control as one. */
    private void setLaunchControlEnabled(boolean enabled) {
        if (binding == null) return;
        binding.launchDock.launchButton.setEnabled(enabled);
        // The chevron opens a picker, which is meaningless while the version manager is not ready,
        // so it is disabled with the face: a live chevron on a dead control is a confusing tap.
        binding.launchDock.launchChevron.setEnabled(enabled);
        binding.launchDock.launchChevron.setAlpha(enabled ? 1f : 0.55f);
    }

    /** Clears the in-flight flag and re-enables the Play button after a launch is abandoned. */
    private void resetLaunchButton() {
        launchInProgress = false;
        setLaunchControlEnabled(true);
        stopLaunchShimmer();
    }

    private void performActualLaunch() {
        // Both the hero card and the Play button call this. Only the button was disabled, so
        // tapping the hero card while a launch was already in flight started a second session —
        // the "duplicate Play" report. One flag guards every entry point.
        if (launchInProgress) return;
        launchInProgress = true;
        // binding is nullable on this screen (resetLaunchButton guards it), so dereferencing it
        // unguarded here threw on the UI thread when the launch was triggered from a path where the
        // view had not been bound yet.
        setLaunchControlEnabled(false);
        LaunchTrace trace = LaunchTrace.create(null);
        trace.milestone("Launch requested");

        final GameVersion version = versionManager != null ? versionManager.getSelectedVersion() : null;

        // The preflight is I/O: stat the package, read the APK's ABI table, hash the extraction
        // marker, parse the signature rules. Running it on the UI thread was the reported "huge lag
        // when Launch is pressed" - the button dimmed and then the frame sat there while the disk
        // was read. Gather on a background thread and hand the decision back to the UI thread; the
        // precedence still lives in LaunchReadiness, so the diagnostics screen and the wizard keep
        // reading the same states.
        final LaunchProbe.Source source = new AndroidLaunchProbe(this, version, trace);
        try {
            launchProbeExecutor.execute(() -> {
                final LaunchReadiness.Facts facts;
                try {
                    facts = LaunchProbe.gather(source);
                } catch (Throwable throwable) {
                    // A probe that throws must not leave the Play button disabled forever: the flag
                    // was set and the button dimmed before the task was submitted, and nothing else
                    // re-arms them. The failure is named through the same path every other launch
                    // failure uses, then the control is released.
                    trace.error("Launch preflight failed", throwable.getMessage());
                    uiHandler.post(() -> reportLaunchFailure(throwable));
                    return;
                }
                uiHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    // The decision path is state work on the UI thread, and a throw anywhere in it
                    // (a dialog that cannot inflate, a launch intent that cannot be built) would
                    // leave the button disabled with the flag still set - the same stuck control
                    // the probe guard above prevents. Re-arming is the floor, not the whole answer.
                    try {
                        continueLaunchAfterProbe(version, trace, facts);
                    } catch (Throwable throwable) {
                        trace.error("Launch decision failed", throwable.getMessage());
                        reportLaunchFailure(throwable);
                    }
                });
            });
        } catch (RejectedExecutionException rejected) {
            // The executor is torn down in onDestroy; a launch requested after that (a queued post
            // from a finishing screen) must re-arm the button rather than leave it disabled.
            trace.error("Launch preflight could not be scheduled", rejected.getMessage());
            resetLaunchButton();
        }
    }

    /**
     * Names a failure that happened on a background step and re-arms the Play control.
     *
     * <p>Shared by the preflight gather and the decision that follows it: both run after the button
     * has already been disabled, so both owe the user the cause and a usable control. The cause is
     * classified rather than printed raw, for the same reason the other launch surfaces do it.
     */
    private void reportLaunchFailure(Throwable throwable) {
        if (isFinishing() || isDestroyed()) return;
        resetLaunchButton();
        LaunchErrorDiagnosis.Cause cause = LaunchErrorDiagnosis.classify(throwable);
        new CustomAlertDialog(this)
                .setTitleText(getString(LaunchErrorDiagnosis.titleRes(cause)))
                .setMessage(getString(LaunchErrorDiagnosis.messageRes(cause))
                        + "\n\n" + getString(R.string.launch_error_cause_label) + " "
                        + LaunchErrorDiagnosis.label(cause)
                        + (throwable.getMessage() != null ? "\n" + throwable.getMessage() : ""))
                .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                .show();
    }

    /**
     * The part of the launch that must run on the UI thread, once the probe has finished.
     *
     * <p>Split out of {@link #performActualLaunch()} so the background gather and the UI decision
     * are visibly separate: everything before this point is I/O, everything after it is state.
     */
    private void continueLaunchAfterProbe(GameVersion version, LaunchTrace trace,
                                          LaunchReadiness.Facts facts) {
        LaunchReadiness.State readiness = LaunchReadiness.evaluate(facts);
        if (LaunchReadiness.blocksLaunch(readiness)) {
            trace.warning("Launch cancelled", LaunchReadiness.key(readiness));
            resetLaunchButton();
            showLaunchFailure(readiness);
            return;
        }
        if (readiness == LaunchReadiness.State.STALE_NATIVE_LIBS) {
            // Repairable and non-fatal: re-extract silently and launch. Telling the user to run
            // Repair for something the launcher can fix itself is the friction this removes.
            trace.mark("Re-extracting stale native libraries");
            try {
                GamePackageManager.invalidateExtractionCache(this, version);
            } catch (Throwable ignored) {
                AppLog.warnOnce("CaramelClient", "MainActivity:2371", "Ignored a failure at MainActivity:2371", ignored);
            }
        }
        if (readiness == LaunchReadiness.State.GAME_NEWER_THAN_RULES) {
            // The installed game is ahead of the signature set. This is a warning, not a
            // blocker: the game runs, only the hook-driven features lag. The user is told once, on
            // the launch attempt that hit it, and continuing is a single tap.
            trace.warning("Installed build newer than signature rules", version.versionCode);
            showNewerGameWarning(version, trace);
            return;
        }

        proceedWithLaunch(version, trace);
    }

    /**
     * The warning shown when the installed build is newer than the signature set.
     *
     * <p>It is deliberately a dismissible dialog rather than a silent log line: the features the
     * user expects (overlays, cosmetics, controller hooks) will be missing, and without a word
     * they would read as a bug in this launcher. It names the newest build the rules cover so the
     * statement is checkable.
     */
    private void showNewerGameWarning(GameVersion version, LaunchTrace trace) {
        String covered = "";
        try {
            covered = SweetLoaderSignatureRulesManager.highestCoveredVersion(this);
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "MainActivity:2399", "Ignored a failure at MainActivity:2399", ignored);
        }
        String message = getString(R.string.launch_failure_message_newer_game);
        if (covered != null && !covered.isEmpty()) {
            message = message + "\n\n"
                    + getString(R.string.launch_failure_newer_game_covered, covered);
        }
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.launch_failure_title_newer_game))
                .setMessage(message)
                .setPositiveButton(getString(R.string.launch_failure_continue_anyway),
                        v -> proceedWithLaunch(version, trace))
                .setOnDismissAnimationEndListener(this::resetLaunchButton)
                .show();
    }

    /**
     * The launch itself, once every readiness check has been satisfied or warned past.
     */
    private void proceedWithLaunch(GameVersion version, LaunchTrace trace) {
        trace.mark("Launch validation completed", version.directoryName + " " + version.versionCode);
        try {
            Intent launchIntent = createMinecraftLaunchIntent();
            launchIntent.putExtra(LaunchTrace.EXTRA_SESSION_ID, trace.getSessionId());
            launchIntent.putExtra(LaunchTrace.EXTRA_STARTED_ELAPSED_MS,
                    android.os.SystemClock.elapsedRealtime() - trace.elapsedMs());
            minecraftLauncher.launch(launchIntent, version, new MinecraftLauncher.LaunchCallback() {
                @Override
                public void onLaunchStarted() {
                    trace.milestone("Loading screen requested");
                }

                @Override
                public void onLaunchFailed(Throwable e) {
                    trace.error("Launch failed before loading screen", e.getMessage());
                    runOnUiThread(() -> {
                        resetLaunchButton();
                    });
                }
            });
        } catch (Throwable e) {
            // Throwable, not Exception: this builds the launch intent and hands it to the launcher,
            // so a heap exhaustion is a real outcome here, and an escaping Error left the Play
            // button disabled with no dialog at all.
            trace.error("Launch failed before activity start", e.getMessage());
            resetLaunchButton();
            // Name the cause instead of only appending the raw dlopen text. The raw message is
            // accurate but does not tell the user what to do; the classifier maps it to the
            // specific reason (Google Play install, wrong bitness, missing dependency, ...) and the
            // raw text is kept beneath it for the bug report.
            LaunchErrorDiagnosis.Cause cause = LaunchErrorDiagnosis.classify(e);
            new CustomAlertDialog(this)
                    .setTitleText(getString(LaunchErrorDiagnosis.titleRes(cause)))
                    .setMessage(getString(LaunchErrorDiagnosis.messageRes(cause))
                            + "\n\n" + getString(R.string.launch_error_cause_label) + " "
                            + LaunchErrorDiagnosis.label(cause)
                            + (e.getMessage() != null ? "\n" + e.getMessage() : ""))
                    .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                    .show();
        }
    }

    /**
     * Shows the dialog for a named failure state (U14).
     *
     * <p>The title and message come from {@link LaunchFailurePresenter}, so every state has copy and
     * the model's warning/blocking decision is the one the UI uses.
     */
    private void showLaunchFailure(LaunchReadiness.State state) {
        int title = LaunchFailurePresenter.titleRes(state);
        int message = LaunchFailurePresenter.messageRes(state);
        if (title == 0 || message == 0) return;

        if (state == LaunchReadiness.State.SIGN_IN_REQUIRED) {
            new CustomAlertDialog(this)
                    .setTitleText(getString(title))
                    .setMessage(getString(message))
                    .setPositiveButton(getString(R.string.go_to_accounts),
                            v -> startActivity(new Intent(this, AccountsActivity.class)))
                    .setNegativeButton(getString(R.string.disable_launcher_login_and_continue),
                            v -> disableLauncherManagedLoginAndLaunch())
                    .show();
            return;
        }
        if (state == LaunchReadiness.State.GAME_NOT_INSTALLED) {
            MinecraftNotFoundDialog.showNotInstalledDialog(this);
            return;
        }
        if (state == LaunchReadiness.State.PLAY_STORE_PACKAGE) {
            // The files are present; they are just not in a form this launcher can open. Send the
            // user to the import flow, which is the actual fix, rather than to the Play Store.
            new CustomAlertDialog(this)
                    .setTitleText(getString(title))
                    .setMessage(getString(message))
                    .setPositiveButton(getString(R.string.import_from_library),
                            v -> startActivity(new Intent(this, LibraryActivity.class)))
                    .setNegativeButton(getString(R.string.dialog_positive_ok), null)
                    .show();
            return;
        }
        new CustomAlertDialog(this)
                .setTitleText(getString(title))
                .setMessage(getString(message))
                .setPositiveButton(getString(R.string.dialog_positive_ok), null)
                .show();
    }

    /**
     * Turns off launcher-managed sign-in and re-runs the launch, which is what the dialog's second
     * button says it does.
     *
     * <p>The button was wired to {@code null}, so choosing it only dismissed the dialog: the flag
     * stayed on, the next launch was refused for exactly the same reason, and the user was left
     * with a button whose label described an action it never performed. Disabling the requirement
     * is the user's explicit decision here, so the flag is cleared and the preflight re-runs with
     * the new state rather than making them press Play again.
     */
    private void disableLauncherManagedLoginAndLaunch() {
        FeatureSettings.getInstance().setLauncherManagedMcLoginEnabled(false);
        Toast.makeText(this, R.string.launcher_login_disabled_launching, Toast.LENGTH_SHORT).show();
        // resetLaunchButton() already ran before the dialog was shown, so the Play button is armed
        // again; re-running the launch picks up the cleared flag.
        performActualLaunch();
    }

    private Intent createMinecraftLaunchIntent() {
        Intent launchIntent = new Intent();
        Intent sourceIntent = getIntent();
        if (sourceIntent == null) return launchIntent;

        if (sourceIntent.hasExtra("MINECRAFT_URI")) {
            launchIntent.putExtra("MINECRAFT_URI", sourceIntent.getStringExtra("MINECRAFT_URI"));
        }
        if (sourceIntent.hasExtra("MINECRAFT_URI_ACTION")) {
            launchIntent.putExtra("MINECRAFT_URI_ACTION", sourceIntent.getStringExtra("MINECRAFT_URI_ACTION"));
        }
        return launchIntent;
    }

     private void showVersionSelectDialog() {
        if (!isVersionManagerReady()) return;

        List<GameVersion> allVersions = new ArrayList<>();
        List<GameVersion> installed = versionManager.getInstalledVersions();
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (installed != null) allVersions.addAll(installed);
        if (custom != null) allVersions.addAll(custom);

        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_instance_selector, null);
        new PersonalizationManager(this).applyAccentToView(popupView, this);
        PopupWindow popup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(16f);
        popup.setOutsideTouchable(true);

        RecyclerView recycler = popupView.findViewById(R.id.recycler_instances);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        GameVersion selectedVersion = versionManager.getSelectedVersion();
        InstancePopupAdapter adapter = new InstancePopupAdapter(allVersions, selectedVersion);
        recycler.setAdapter(adapter);

        android.widget.EditText searchInput = popupView.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        adapter.setOnItemClickListener(version -> {
            versionManager.selectVersion(version);
            viewModel.setCurrentVersion(version);
            setTextMinecraftVersion();
            popup.dismiss();
        });

        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();
        View anchor = binding.launchDock.selectVersionButton;
        // The picker sits at the bottom of the screen, so a drop-down popup would open off-screen.
        // Lift it above the anchor, into the page area, where the instance list has room to render.
        int xOffset = Math.max(0, (anchor.getWidth() - popupWidth) / 2);
        int yOffset = -(anchor.getHeight() + popupHeight + 8);
        popup.showAsDropDown(anchor, xOffset, yOffset);
    }

    private static class InstancePopupAdapter extends RecyclerView.Adapter<InstancePopupAdapter.VH> {
        private final List<GameVersion> allVersions;
        private List<GameVersion> filteredVersions;
        private final GameVersion selectedVersion;
        private OnItemClickListener listener;

        interface OnItemClickListener {
            void onClick(GameVersion version);
        }

        void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

        InstancePopupAdapter(List<GameVersion> versions, GameVersion selected) {
            this.allVersions = versions;
            this.filteredVersions = new ArrayList<>(versions);
            this.selectedVersion = selected;
        }

        void filter(String query) {
            List<GameVersion> next = new ArrayList<>();
            if (query == null || query.isEmpty()) {
                next.addAll(allVersions);
            } else {
                String q = query.toLowerCase();
                for (GameVersion v : allVersions) {
                    String name = getInstanceDisplayName(v).toLowerCase();
                    String code = v.versionCode != null ? v.versionCode.toLowerCase() : "";
                    String dir = v.directoryName != null ? v.directoryName.toLowerCase() : "";
                    if (name.contains(q) || code.contains(q) || dir.contains(q)) {
                        next.add(v);
                    }
                }
            }
            // The filtered list is a subset of the same rows, so diffing keeps the rows that are
            // still shown and animates the ones that a keystroke removed. A dataset change here
            // rebuilt the whole popup on every character typed.
            ListDiff.Result diff = ListDiff.compute(filteredVersions, next,
                    version -> version.directoryName,
                    (old, fresh) -> java.util.Objects.equals(old.displayName, fresh.displayName)
                            && java.util.Objects.equals(old.versionCode, fresh.versionCode));
            filteredVersions = next;
            ListDiffDispatch.apply(this, diff);
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_instance_popup, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            GameVersion v = filteredVersions.get(position);
            boolean isSelected = selectedVersion != null
                    && selectedVersion.directoryName != null
                    && selectedVersion.directoryName.equals(v.directoryName);

            holder.name.setText(getInstanceDisplayName(v));
            String versionText = getInstanceVersionText(v);
            holder.version.setText(versionText);
            holder.version.setVisibility(TextUtils.isEmpty(versionText) ? View.GONE : View.VISIBLE);
            String abi = v.abiList;
            if (abi != null && !abi.isEmpty()) {
                boolean is32Bit = "armeabi-v7a".equals(abi) || "x86".equals(abi);
                holder.abiBadge.setText(holder.itemView.getContext().getString(
                        is32Bit ? R.string.abi_32_bit : R.string.abi_64_bit));
                holder.abiBadge.setTextColor(is32Bit ? 0xFFF6A821 : 0xFF0F5BE4);
                holder.abiBadge.setVisibility(View.VISIBLE);
            } else {
                holder.abiBadge.setVisibility(View.GONE);
            }
            holder.itemView.setActivated(isSelected);
            holder.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.tag.setVisibility(View.GONE);
            applyInstanceSelectionStyle(holder, isSelected);

            holder.itemView.setOnClickListener(_v -> {
                if (listener != null) listener.onClick(v);
            });
        }

        private static void applyInstanceSelectionStyle(@NonNull VH holder, boolean isSelected) {
            android.content.Context context = holder.itemView.getContext();
            PersonalizationManager pm = new PersonalizationManager(context);
            int accent = pm.getAccentColor();
            if (accent == 0) {
                accent = ContextCompat.getColor(context, R.color.primary);
            }

            float density = context.getResources().getDisplayMetrics().density;
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(10 * density);
            if (isSelected) {
                background.setColor(Color.argb(26, Color.red(accent), Color.green(accent), Color.blue(accent)));
                background.setStroke(Math.max(1, (int) (1 * density)), accent);
            } else {
                background.setColor(Color.TRANSPARENT);
                background.setStroke(0, Color.TRANSPARENT);
            }
            holder.itemView.setBackground(background);
            holder.check.setImageTintList(ColorStateList.valueOf(accent));
        }

        @Override public int getItemCount() { return filteredVersions.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, version, tag;
            TextView abiBadge;
            ImageView check;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.instance_name);
                version = v.findViewById(R.id.instance_version);
                tag = v.findViewById(R.id.instance_tag);
                abiBadge = v.findViewById(R.id.instance_abi_badge);
                check = v.findViewById(R.id.instance_check);
            }
        }
    }

    private void startFilePicker(String type, ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        launcher.launch(intent);
    }

    private void startApkFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/zip",
                "application/xapk",
                "application/x-xapk"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        apkImportResultLauncher.launch(intent);
    }

    private void openContentManagement() {
        GameVersion currentVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        if (currentVersion == null) {
            Toast.makeText(this, getString(R.string.not_found_version), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ContentManagementActivity.class);
        startActivity(intent);
    }


    public void setTextMinecraftVersion() {
        if (binding == null) return;
        if (versionManager == null) {
            binding.launchDock.textMinecraftVersion.setText(getString(R.string.not_found_version));
            binding.launchDock.launchButtonSubtitle.setText(getString(R.string.not_found_version));
            updateLastPlayedName(null, null);
            return;
        }
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        String instanceName = selectedVersion != null ? getInstanceDisplayName(selectedVersion) : null;
        binding.launchDock.textMinecraftVersion.setText(TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName);
        // The split button names the build it will start, so the action is unambiguous without
        // reading the picker above it.
        binding.launchDock.launchButtonSubtitle.setText(TextUtils.isEmpty(instanceName)
                ? getString(R.string.not_found_version) : instanceName);
        updateLastPlayedName(instanceName, selectedVersion);
        refreshLastPlayedCard();
    }

    private void refreshLastPlayedCard() {
        GameVersion selectedVersion = versionManager != null ? versionManager.getSelectedVersion() : null;
        int installedCount =0;
        if (versionManager != null) {
            List<org.chimeramc.client.core.versions.GameVersion> installed = versionManager.getInstalledVersions();
            List<org.chimeramc.client.core.versions.GameVersion> custom = versionManager.getCustomVersions();
            installedCount = (installed != null ? installed.size() : 0) + (custom != null ? custom.size() : 0);
        }
        // The hero's stats are rendered by the Compose hero; refreshHero recomposes them from the
        // live values, so the count-up below only drives the animated number into that state.
        final int count = installedCount;
        DynamicAnim.countUp(count, 400L, value -> {
            heroInstancesCount = (int) value;
            refreshHero();
        });
        // The mods count comes from the live mod list, not from a count captured here: the list
        // loads asynchronously per instance, so reading it at bind time would report the previous
        // instance (usually zero). updateModsUI recomputes it whenever the list changes.
        if (viewModel != null) {
            refreshActiveModsStat(viewModel.getModsLiveData().getValue());
        }
        long totalPlaytime = selectedVersion != null
                ? PlaytimeManager.getTotalMs(MinecraftLauncher.getStorageProfileId(selectedVersion))
                : 0L;
        heroPlaytime = PlaytimeManager.formatPlaytime(totalPlaytime);
        updateLastPlayedName(
                selectedVersion != null ? getInstanceDisplayName(selectedVersion) : null,
                selectedVersion
        );
    }

    private void updateLastPlayedName(String instanceName, GameVersion selectedVersion) {
        heroName = TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName;
        String versionText = selectedVersion != null ? getInstanceVersionText(selectedVersion) : "";
        // Instances are commonly named after their version, so the same "26.51" would appear twice.
        // The rule lives in LaunchHeroState so it is unit-tested; the hero renders the result.
        heroVersion = org.chimeramc.client.ui.compose.launch.LaunchHeroState
                .versionOrEmpty(heroName, versionText);
        heroAbi = org.chimeramc.client.ui.compose.launch.LaunchHeroState
                .abiKind(selectedVersion != null ? selectedVersion.abiList : null);
        refreshHero();
    }

    private boolean isVersionManagerReady() {
        if (versionManager != null) return true;
        Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
        return false;
    }

    private static String getInstanceDisplayName(GameVersion version) {
        if (version == null) return "";

        String displayName = stripVersionSuffix(version.displayName, version.versionCode);
        if (!TextUtils.isEmpty(displayName)) return displayName;
        if (!TextUtils.isEmpty(version.directoryName)) return version.directoryName;
        return !TextUtils.isEmpty(version.versionCode) ? version.versionCode : "";
    }

    private static String getInstanceVersionText(GameVersion version) {
        if (version == null) return "";
        String base = !TextUtils.isEmpty(version.versionCode) ? version.versionCode
                : !TextUtils.isEmpty(version.directoryName) ? version.directoryName : "";
        String abi = version.abiList;
        if (!TextUtils.isEmpty(abi) && ("armeabi-v7a".equals(abi) || "x86".equals(abi))) {
            return base + " (" + abi + ")";
        }
        return base;
    }

    private static String stripVersionSuffix(String displayName, String versionCode) {
        if (TextUtils.isEmpty(displayName)) return "";

        String trimmedName = displayName.trim();
        if (TextUtils.isEmpty(versionCode)) return trimmedName;

        String suffix = " (" + versionCode + ")";
        if (trimmedName.endsWith(suffix)) {
            return trimmedName.substring(0, trimmedName.length() - suffix.length()).trim();
        }
        return trimmedName;
    }

    private void handleIncomingFiles() {
        if (fileHandler == null) return;
        Intent intent = getIntent();
        if (MinecraftImportIntents.isMinecraftResourceIntent(this, intent)) {
            return;
        }
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if ("minecraft".equals(data.getScheme())) {
                return;
            }
        }
        fileHandler.processIncomingFilesWithConfirmation(intent, new IncomingCallback(), false);
    }

    /** Handles the normal Bedrock mod import and reports the outcome. */
    private final class IncomingCallback implements FileHandler.FileOperationCallback {
        @Override
        public void onSuccess(int processedFiles) {
            hideProgressLoader();
            if (processedFiles > 0)
                UIHelper.showToast(MainActivity.this, getResources().getQuantityString(R.plurals.files_processed, processedFiles, processedFiles));
        }

        @Override
        public void onError(String errorMessage) {
            hideProgressLoader();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                UIHelper.showToast(MainActivity.this, errorMessage);
            }
        }

        @Override
        public void onProgressUpdate(int progress) {
            if (binding != null) animateProgressTo(progress);
        }
    }

    private boolean forwardIncomingMinecraftResourceToRunningGame() {
        Intent intent = getIntent();
        if (!MinecraftImportIntents.isMinecraftResourceIntent(this, intent)) {
            return false;
        }
        if (!MinecraftImportIntents.forwardToRunningMinecraft(this, intent)) {
            return false;
        }

        clearIncomingIntent();
        return true;
    }

    private void clearIncomingIntent() {
        Intent cleanIntent = new Intent(this, MainActivity.class);
        setIntent(cleanIntent);
    }

    private void handleMinecraftUriLaunch() {
        Intent intent = getIntent();
        if (intent == null) return;
        if (intent.getBooleanExtra("LAUNCH_WITH_URI", false)) {
            intent.removeExtra("LAUNCH_WITH_URI");
            setIntent(intent);
            binding.getRoot().post(this::launchGame);
        }
    }

    private void updateModsUI(List<Mod> mods) {
        if (binding == null) return;

        // The per-instance "N active" stat is derived from this same list. Mod discovery runs
        // off the UI thread and the version is bound after the live data is observed, so the
        // count read at bind time is usually the pre-load value (zero). Recompute it here so
        // the hero card reflects the instance that is actually selected.
        refreshActiveModsStat(mods);

    }

    private void refreshActiveModsStat(List<Mod> mods) {
        int count = Mod.countEnabled(mods);
        DynamicAnim.countUp(count, 400L, value -> {
            heroModsCount = (int) value;
            refreshHero();
        });
    }

    /**
     * Builds the Launch-tab hero once and hosts it in its ComposeView.
     *
     * <p>The hero is a Compose screen now, but it is still the real character preview: the host binds
     * the equipped cape/hat/pet from the CosmeticStore (which the composable cannot read) and starts
     * the preview, then keeps the [LaunchHeroState] in step so a recomposition shows the new values.
     */
    private void setupLaunchHero() {
        setupLaunchPreviewStage();
        View host = findViewById(R.id.launch_hero_compose);
        if (!(host instanceof androidx.compose.ui.platform.ComposeView)) return;
        heroCompose = (androidx.compose.ui.platform.ComposeView) host;
        heroState = org.chimeramc.client.ui.compose.launch.LaunchHeroHost.state(buildHeroState());
        heroCompose.setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        org.chimeramc.client.ui.compose.launch.LaunchHeroHost.setContent(
                heroCompose,
                heroState,
                this::openProfileSheet,
                this::launchGame,
                view -> {
                    launchPreview = view;
                    bindLaunchPreviewCosmetics();
                    if (launchPreview != null) launchPreview.startPreview();
                });
    }

    /**
     * Mounts the preview stage and its background picker.
     *
     * <p>The stage shows the character centred on the saved background; a long-press reveals the
     * picker, which shows each background and stores the choice by its stable id. The character view
     * is still the real CapePreviewView, handed back through onPreviewReady so the activity binds
     * the equipped cosmetics.
     */
    private void setupLaunchPreviewStage() {
        View stageHost = binding.launchPreviewStage;
        if (!(stageHost instanceof androidx.compose.ui.platform.ComposeView)) return;
        String saved = org.chimeramc.client.settings.FeatureSettings.getInstance()
                .getLaunchBackgroundId();
        backgroundState = org.chimeramc.client.ui.compose.launch.LaunchPreviewHost
                .state(org.chimeramc.client.ui.compose.launch.PreviewBackground.resolve(saved).id);
        androidx.compose.ui.platform.ComposeView stage =
                (androidx.compose.ui.platform.ComposeView) stageHost;
        stage.setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        org.chimeramc.client.ui.compose.launch.LaunchPreviewHost.setStageContent(
                stage,
                backgroundState,
                getString(R.string.cosmetics_preview_description),
                this::toggleBackgroundPicker,
                view -> {
                    launchPreview = view;
                    bindLaunchPreviewCosmetics();
                    if (launchPreview != null) launchPreview.startPreview();
                });

        View pickerHost = binding.launchBackgroundPicker;
        if (pickerHost instanceof androidx.compose.ui.platform.ComposeView) {
            backgroundPickerCompose = (androidx.compose.ui.platform.ComposeView) pickerHost;
            backgroundPickerCompose.setViewCompositionStrategy(
                    androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
            org.chimeramc.client.ui.compose.launch.LaunchPreviewHost.setPickerContent(
                    backgroundPickerCompose,
                    backgroundState,
                    id -> org.chimeramc.client.settings.FeatureSettings.getInstance()
                            .setLaunchBackgroundId(id));
        }
    }

    /** Shows or hides the background picker; the long-press on the stage calls this. */
    private void toggleBackgroundPicker() {
        View picker = binding.launchBackgroundPicker;
        if (picker == null) return;
        boolean show = picker.getVisibility() != View.VISIBLE;
        picker.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            picker.setAlpha(0f);
            picker.animate().alpha(1f).setDuration(180L).start();
        }
    }

    /** Rebuilds the hero state from the live fields and recomposes. */
    private void refreshHero() {
        if (heroState == null) return;
        heroState.setValue(buildHeroState());
    }

    private org.chimeramc.client.ui.compose.launch.LaunchHeroState buildHeroState() {
        return new org.chimeramc.client.ui.compose.launch.LaunchHeroState(
                getString(R.string.last_played_title),
                getString(R.string.last_played_tap),
                heroName.isEmpty() ? getString(R.string.not_found_version) : heroName,
                heroVersion,
                heroAbi,
                heroInstancesCount,
                heroModsCount,
                heroPlaytime,
                getString(R.string.cosmetics_preview_description));
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_launch);
        findViewById(R.id.nav_tab_launch).setOnClickListener(v -> {});
    }

    @Override
    protected void onDestroy() {
        // The Compose hero releases its own preview when the composition leaves, so there is no
        // manual release here: doing both would release the same view twice.
        launchPreview = null;
        unbindStorageMigrationService();
        dismissStorageMigrationDialog(null);
        // Both managers own a single-thread pool; without this they outlived the Activity.
        if (storageMigrationManager != null) {
            storageMigrationManager.close();
            storageMigrationManager = null;
        }
        if (apkImportManager != null) {
            apkImportManager.release();
            apkImportManager = null;
        }
        storageMigrationExecutor.shutdownNow();
        launchProbeExecutor.shutdownNow();
        resourcepackExecutor.shutdownNow();
        launchStatsExecutor.shutdownNow();
        // The account executor was never shut down either, so it outlived the Activity.
        accountExecutor.shutdownNow();
        // Removes a pending tip rotation, so it cannot fire into the destroyed Activity.
        tipScope.close();
        if (announcementClient != null) {
            announcementClient.shutdown();
            announcementClient = null;
        }
    }

    /**
     * Submits account work, tolerating an executor {@link #onDestroy()} has shut down.
     *
     * <p>Every call site is reached from a UI event - a sign-in tap, an account switch, an avatar
     * bind - so a submit that raced the teardown threw
     * {@link java.util.concurrent.RejectedExecutionException} on the main thread and took the Launch
     * tab down. The work is best-effort (a refresh or an avatar decode), so dropping it is safe.
     */
    private void runOnAccountExecutor(Runnable work) {
        // A false return means the screen is gone; there is nothing to refresh.
        SafeExecutor.submit(accountExecutor, work);
    }

}
