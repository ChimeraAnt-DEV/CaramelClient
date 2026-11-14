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

    private com.microsoft.xbox.idp.toolkit.CircleImageView accountAvatar;
    private View accountAvatarContainer;
    private ProgressBar avatarProgress;
    private Button signInButton;
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

    /** The lifetime of the self-reposting tip rotation, closed in onDestroy. */
    private final MainThread.Scope tipScope = MainThread.scope();
    private Runnable pulseRunnable;
    private boolean pulseRunning;
    private View launchShimmer;
    private Runnable shimmerRunnable;
    private boolean shimmerRunning;
    /** The Caramel launch screen's state, and the ComposeView that hosts the whole tab. */
    private androidx.compose.runtime.MutableState<org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState> launchState;
    private androidx.compose.runtime.MutableState<java.util.List<org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle>> toggleState;
    private androidx.compose.runtime.MutableState<Integer> worldsCount;
    private androidx.compose.runtime.MutableState<Integer> packsCount;
    private androidx.compose.runtime.MutableState<Integer> behaviorsCount;
    private androidx.compose.ui.platform.ComposeView launchCompose;
    /** The selected instance's name, the line the launch face shows, and its version id. */
    private String heroName = "";
    private String heroBuildLine = "";
    private String heroVersionId = "";
    /** The active mod count, kept for anything that reads it (not drawn on the Caramel screen). */
    private int heroModsCount;
    /** Whether the Compose launch control accepts a tap. */
    private boolean launchEnabled;
    /** The live character preview inside the Caramel hero, owned by the composition. */
    private org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView launchPreview;
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

    private void animateHomeCardsArrival() {
        // The whole tab is one Compose screen now, so there are no per-block views to stagger. The
        // screen fades in as a unit instead, which is the entrance the pack's desktop window uses.
        if (binding == null || binding.getRoot() == null) return;
        if (!DynamicAnim.areAnimationsEnabled()) return;
        View root = binding.getRoot();
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(LaunchIntro.BLUR_FADE_MS)
                .setInterpolator(DynamicAnim.getDefaultInterpolator()).start();
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
        MsftAccountStore.MsftAccount active = getActiveAccount();
        if (active == null) {
            if (signInButton != null) signInButton.setVisibility(View.VISIBLE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.GONE);
            if (accountAvatar != null) accountAvatar.setImageDrawable(null);
            lastAvatarXuid = null;
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
        } else {
            if (signInButton != null) signInButton.setVisibility(View.GONE);
            if (accountAvatarContainer != null) accountAvatarContainer.setVisibility(View.VISIBLE);
            loadXboxAvatar(active);
        }
        // The Compose title bar draws the account pill, so the same state is pushed there.
        refreshLaunchState();
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
            if (avatarProgress != null) avatarProgress.setVisibility(View.GONE);
            refreshLaunchState();
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
                            refreshLaunchState();
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
        // The Caramel screen owns its own shape scale (the pack's radii), so the corner-radius
        // preference no longer retunes a dock drawable here. The preference still drives the View
        // screens; this screen reads none of it, which is the point of a single design system.
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
        // The Caramel screen draws its own glass shadows (GlassPanel), so there is no dock or card
        // view here to raise. The elevation values stay for the View screens that still use them.
    }
    
    private void startHeroCardPulse() {
        // The hero is Compose now and draws its own focus state; there is no View to pulse.
    }

    private void stopHeroCardPulse() {
        pulseRunning = false;
        if (pulseRunnable != null) {
            uiHandler.removeCallbacks(pulseRunnable);
            pulseRunnable = null;
        }
    }



    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onStop() {
        stopHeroCardPulse();
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
        // The launch control lives in the Compose screen now; its callbacks are wired in
        // setupCaramelLaunchScreen(), so there is no dock view to bind here.
        setupAccountHeader();
        FeatureSettings.init(getApplicationContext());
        // One mod-update notification a day, never a prompt per mod. The scheduler applies the
        // feature switch and the throttle, and does nothing at all when this build has no feed.
        org.chimeramc.client.core.mods.update.ModUpdateScheduler.requestCheck(this);
    }







    /** Rebuilds the Caramel quick toggles from the live statuses and recomposes. */
    private void refreshToggleRow() {
        if (toggleState == null) return;
        toggleState.setValue(buildQuickToggles());
    }

    /** The quick toggles the Caramel screen draws: Mods, Voice and Replay, with live statuses. */
    private java.util.List<org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle>
            buildQuickToggles() {
        boolean guest = org.chimeramc.client.core.privacy.PrivacyModes.current(this).isGuestMode();
        java.util.List<org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle> list =
                new java.util.ArrayList<>();
        if (!guest) {
            list.add(new org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle(
                    "mods", getString(R.string.launch_toggle_mods), modsStatusText,
                    !TextUtils.isEmpty(modsStatusText), true));
        }
        list.add(new org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle(
                "voice", getString(R.string.launch_toggle_voice), voiceStatusText,
                voiceStatusText != null && voiceStatusText.contains("on"), true));
        list.add(new org.chimeramc.client.ui.compose.caramel.launch.CaramelQuickToggle(
                "replay", getString(R.string.launch_toggle_replay), replayStatusText,
                !TextUtils.isEmpty(replayStatusText), true));
        return list;
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
        // The account pill in the Compose title bar drives sign-in and account switching through the
        // launch screen's actions, so there is no separate header view to wire here.
        refreshAccountHeaderUI();
    }





    private void initContentManagementSection() {
        // The Caramel screen shows the three counts as cards and opens the list on a tap, so the
        // activity only has to keep the counts in its Compose state. These observers feed the three
        // MutableState<Int> values the screen reads.
        contentManager = ContentManager.getInstance(this);
        contentManager.getWorldsLiveData().observe(this, worlds ->
                setCount(worldsCount, worlds != null ? worlds.size() : 0));
        contentManager.getResourcePacksLiveData().observe(this, packs ->
                setCount(packsCount, packs != null ? packs.size() : 0));
        contentManager.getBehaviorPacksLiveData().observe(this, behaviors ->
                setCount(behaviorsCount, behaviors != null ? behaviors.size() : 0));
    }

    /** Pushes a count into its Compose state, tolerating a screen that is not mounted yet. */
    private static void setCount(androidx.compose.runtime.MutableState<Integer> state, int value) {
        if (state != null) state.setValue(value);
    }

    private void refreshContentCounts() {
        // The counts arrive through the LiveData observers in initContentManagementSection and are
        // pushed straight into the Compose state, so there is nothing to recompute here.
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


    private void initMiscellaneousSection() {
        // The Miscellaneous rows (CurseForge, Mod Hub, Accounts, Quick Launch) are the Compose title
        // bar's app menu now, wired through the launch screen's actions. Nothing to bind here.
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

    private void openModsFullscreen() {
        Intent intent = new Intent(this, ModsFullscreenActivity.class);
        startActivity(intent);
    }

    private void launchGame() {
        if (!isVersionManagerReady()) return;
        performActualLaunch();
    }

    /** Enables or disables the Compose launch control as one. */
    private void setLaunchControlEnabled(boolean enabled) {
        launchEnabled = enabled;
        refreshLaunchState();
    }

    /** Clears the in-flight flag and re-enables the Play button after a launch is abandoned. */
    private void resetLaunchButton() {
        launchInProgress = false;
        setLaunchControlEnabled(true);
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
        if (versionManager == null) {
            heroName = getString(R.string.not_found_version);
            heroBuildLine = "";
            heroVersionId = "";
            updateLastPlayedName(null, null);
            refreshLaunchState();
            return;
        }
        GameVersion selectedVersion = versionManager.getSelectedVersion();
        String instanceName = selectedVersion != null ? getInstanceDisplayName(selectedVersion) : null;
        heroName = TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName;
        // The split button names the build it will start, so the action is unambiguous without
        // reading the picker above it.
        heroBuildLine = TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName;
        heroVersionId = selectedVersion != null && !TextUtils.isEmpty(selectedVersion.versionCode)
                ? selectedVersion.versionCode : "";
        updateLastPlayedName(instanceName, selectedVersion);
        refreshLastPlayedCard();
        refreshLaunchState();
    }

    private void refreshLastPlayedCard() {
        // The Caramel screen has no stat strip to count up; the version manager's refresh path
        // calls setTextMinecraftVersion(), which pushes the state the screen draws.
        refreshLaunchState();
    }

    private void updateLastPlayedName(String instanceName, GameVersion selectedVersion) {
        heroName = TextUtils.isEmpty(instanceName) ? getString(R.string.not_found_version) : instanceName;
        // The version line only earns its place when it adds something beyond the instance name.
        // The rule lives in LaunchHeroState so it stays unit-tested.
        String versionText = selectedVersion != null ? getInstanceVersionText(selectedVersion) : "";
        heroBuildLine = org.chimeramc.client.ui.compose.launch.LaunchHeroState
                .versionOrEmpty(heroName, versionText);
        if (TextUtils.isEmpty(heroBuildLine)) heroBuildLine = heroVersionId;
        refreshLaunchState();
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
            if (processedFiles > 0)
                UIHelper.showToast(MainActivity.this, getResources().getQuantityString(R.plurals.files_processed, processedFiles, processedFiles));
        }

        @Override
        public void onError(String errorMessage) {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                UIHelper.showToast(MainActivity.this, errorMessage);
            }
        }

        @Override
        public void onProgressUpdate(int progress) {
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
        // The Caramel launch screen has no active-mod stat (the pack's hero carries no stat strip),
        // so this only keeps the count for anything that reads it later.
        heroModsCount = Mod.countEnabled(mods);
    }






    /** Called by the hero composition when the real character preview is created. */
    private void onLaunchPreviewReady(
            org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView view) {
        launchPreview = view;
        bindLaunchPreviewCosmetics();
        if (launchPreview != null) launchPreview.startPreview();
    }

    /**
     * Applies the player's equipped cosmetics to the hero preview, using the same {@code
     * CosmeticStore} the Cosmetics panel writes, so the hero shows exactly what the game will render
     * (or the Optifine default cape when Optifine Mode is on).
     */
    private void bindLaunchPreviewCosmetics() {
        if (launchPreview == null) return;
        try {
            org.chimeramc.client.core.cosmetics.CosmeticStore store =
                    new org.chimeramc.client.core.cosmetics.CosmeticStore(this);
            launchPreview.setCape(store.getEquippedCapeForDisplay());
            launchPreview.setAccessory(store.getEquippedAccessory());
            launchPreview.setPet(store.getEquippedPet());
            launchPreview.refreshSkin();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "MainActivity:preview",
                    "Ignored a failure binding the hero preview cosmetics", ignored);
        }
    }

    private void setupNavBar() {
        setActiveNavTab(R.id.nav_tab_launch);
        findViewById(R.id.nav_tab_launch).setOnClickListener(v -> {});
    }

    /**
     * Mounts the Caramel launch screen — the whole Launch tab.
     *
     * <p>One ComposeView renders the entire page (the pack's title bar, sidebar and hero). The
     * activity keeps the state and the actions; everything Compose-shaped stays in Kotlin, behind
     * {@link org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost}.
     */
    private void setupCaramelLaunchScreen() {
        View host = findViewById(R.id.launch_content);
        if (!(host instanceof androidx.compose.ui.platform.ComposeView)) return;
        launchCompose = (androidx.compose.ui.platform.ComposeView) host;
        launchState = org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.state(
                buildLaunchState());
        toggleState = org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.toggleState(
                buildQuickToggles());
        worldsCount = org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.countState(0);
        packsCount = org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.countState(0);
        behaviorsCount = org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.countState(0);
        launchCompose.setViewCompositionStrategy(
                androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.setContent(
                launchCompose, launchState, toggleState, worldsCount, packsCount, behaviorsCount,
                this::onLaunchPreviewReady, launchActions);
        setLaunchControlEnabled(false);
    }

    /** Rebuilds the launch screen's state from the live fields and recomposes. */
    private void refreshLaunchState() {
        if (launchState == null) return;
        launchState.setValue(buildLaunchState());
    }

    private org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState buildLaunchState() {
        java.util.List<org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState.Version>
                versions = new java.util.ArrayList<>();
        if (versionManager != null) {
            java.util.List<GameVersion> installed = versionManager.getInstalledVersions();
            if (installed != null) {
                for (GameVersion v : installed) {
                    versions.add(versionRow(v));
                }
            }
            java.util.List<GameVersion> custom = versionManager.getCustomVersions();
            if (custom != null) {
                for (GameVersion v : custom) {
                    versions.add(versionRow(v));
                }
            }
        }
        MsftAccountStore.MsftAccount active = getActiveAccount();
        android.graphics.Bitmap avatar = null;
        if (active != null) {
            String url = org.chimeramc.client.util.AccountTextUtils.sanitizeUrl(active.xboxAvatarUrl);
            if (url != null) avatar = org.chimeramc.client.util.AccountTextUtils.getCachedAvatar(url);
        }
        return new org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState(
                heroName,
                heroBuildLine,
                heroVersionId,
                versions,
                launchEnabled && !launchInProgress,
                launchInProgress,
                active != null
                        ? org.chimeramc.client.util.AccountTextUtils.displayNameOrNotSigned(this, active)
                        : "",
                avatar,
                org.chimeramc.client.core.minecraft.MinecraftActivityState.isRunning(this) ? 1 : 0,
                org.chimeramc.client.ui.compose.launch.PreviewBackground
                        .resolve(org.chimeramc.client.settings.FeatureSettings.getInstance()
                                .getLaunchBackgroundId())
                        .drawableRes);
    }

    /** One version row: the id shown and whether it is a beta build. */
    private static org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState.Version versionRow(
            GameVersion v) {
        String id = !TextUtils.isEmpty(v.versionCode) ? v.versionCode
                : !TextUtils.isEmpty(v.directoryName) ? v.directoryName : "";
        boolean beta = id.toLowerCase(java.util.Locale.ROOT).contains("beta");
        return new org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchState.Version(id, beta);
    }

    /** The launch screen's callbacks; every one is a real launcher action. */
    private final org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.Actions launchActions =
            new org.chimeramc.client.ui.compose.caramel.launch.CaramelLaunchHost.Actions() {
        @Override public void launch() { launchGame(); }
        @Override public void selectVersion(String id) { selectVersionById(id); }
        @Override public void openAccount() { showAccountSwitchPopup(findViewById(R.id.launch_root)); }
        @Override public void openMods() { openModsFullscreen(); }
        @Override public void openCosmetics() { openProfileSheet(); }
        @Override public void openSettings() {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        @Override public void openContent() { openContentManagement(); }
        @Override public void openAccounts() {
            startActivity(new Intent(MainActivity.this, AccountsActivity.class));
        }
        @Override public void openCurseForge() {
            startActivity(new Intent(MainActivity.this, CurseForgeActivity.class));
        }
        @Override public void openModHub() {
            startActivity(new Intent(MainActivity.this, ModHubActivity.class));
        }
        @Override public void openQuickLaunch() {
            startActivity(new Intent(MainActivity.this, QuickLaunchActivity.class));
        }
        @Override public void cycleBackground() {
            org.chimeramc.client.settings.FeatureSettings settings =
                    org.chimeramc.client.settings.FeatureSettings.getInstance();
            settings.setLaunchBackgroundId(org.chimeramc.client.ui.compose.launch.PreviewBackground
                    .next(settings.getLaunchBackgroundId()));
            refreshLaunchState();
        }
        @Override public void toggle(String id, boolean on) {
            // The quick toggles are shortcuts, not switches: a tap opens the feature. The card's
            // toggle reflects the feature's live state, so it is refreshed, not flipped here.
            switch (id) {
                case "mods": openModsFullscreen(); break;
                case "voice":
                    startActivity(new Intent(MainActivity.this,
                            org.chimeramc.client.ui.activities.VoiceChatActivity.class));
                    break;
                case "replay":
                    LibraryActivity.open(MainActivity.this, LibraryActivity.TAB_REPLAY);
                    break;
                default: break;
            }
        }
    };

    /** Selects the installed version whose id matches, then refreshes the screen. */
    private void selectVersionById(String id) {
        if (versionManager == null || TextUtils.isEmpty(id)) return;
        GameVersion match = null;
        for (GameVersion v : versionManager.getInstalledVersions()) {
            if (id.equals(v.versionCode) || id.equals(v.directoryName)) { match = v; break; }
        }
        if (match == null) {
            for (GameVersion v : versionManager.getCustomVersions()) {
                if (id.equals(v.versionCode) || id.equals(v.directoryName)) { match = v; break; }
            }
        }
        if (match == null) return;
        versionManager.selectVersion(match);
        setTextMinecraftVersion();
    }

    @Override
    protected void onDestroy() {
        // The composition releases the preview on the way out; clearing the reference stops the
        // resume path from touching a released view.
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
