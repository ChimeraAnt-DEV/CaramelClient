package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.chimeramc.client.R;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.fragments.ControllerSettingsFragment;
import org.chimeramc.client.ui.fragments.SkinsSettingsFragment;
import org.chimeramc.client.util.PersonalizationManager;

/**
 * Hosts Controller and Skins behind one "Customize" tab.
 *
 * Seven top-level tabs did not fit a phone once the news bell and account avatar took their
 * space, so the two personalisation screens share this host with an inner segmented control.
 * The two fragments reuse the existing activity layouts, which keeps every view id and
 * behaviour intact.
 */
public class CustomizeActivity extends BaseActivity {

    private static final String TAG_CONTROLLER = "customize_controller";
    private static final String TAG_SKINS = "customize_skins";

    /** Which inner tab to open on create. */
    public static final String EXTRA_INITIAL_TAB = "customize_initial_tab";
    public static final int TAB_CONTROLLER = 0;
    public static final int TAB_SKINS = 1;
    public static final int TAB_ACCOUNT = 2;

    private TextView controllerTab;
    private TextView skinsTab;
    private TextView accountTab;
    private androidx.compose.ui.platform.ComposeView accountCompose;
    private androidx.compose.runtime.MutableState<org.chimeramc.client.ui.compose.account.AccountSectionState> accountState;
    private final org.chimeramc.client.core.xbox.XboxFriendsClient friendsClient =
            new org.chimeramc.client.core.xbox.XboxFriendsClient();
    private org.chimeramc.client.core.auth.MsftAccountStore.MsftAccount activeAccount;
    private int accent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);

        PersonalizationManager pm = new PersonalizationManager(this);
        accent = pm.getAccentColor();
        View root = findViewById(R.id.customize_root);
        if (root != null) {
            pm.applyAccentToView(root, this);
        }

        controllerTab = findViewById(R.id.customize_tab_controller);
        skinsTab = findViewById(R.id.customize_tab_skins);
        accountTab = findViewById(R.id.customize_tab_account);
        controllerTab.setOnClickListener(v -> showController());
        skinsTab.setOnClickListener(v -> showSkins());
        accountTab.setOnClickListener(v -> showAccount());
        DynamicAnim.applyPressScale(controllerTab);
        DynamicAnim.applyPressScale(skinsTab);
        DynamicAnim.applyPressScale(accountTab);

        int initialTab = getIntent() == null
                ? TAB_CONTROLLER
                : getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_CONTROLLER);
        if (savedInstanceState == null) {
            if (initialTab == TAB_SKINS) {
                showSkins();
            } else if (initialTab == TAB_ACCOUNT) {
                showAccount();
            } else {
                showController();
            }
        } else {
            boolean skinsVisible = getSupportFragmentManager().findFragmentByTag(TAG_SKINS) != null;
            styleTabs(!skinsVisible);
        }
    }

    private void showController() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_CONTROLLER) == null) {
            replace(new ControllerSettingsFragment(), TAG_CONTROLLER);
        }
        styleTabs(true);
    }

    private void showSkins() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_SKINS) == null) {
            replace(new SkinsSettingsFragment(), TAG_SKINS);
        }
        styleTabs(false);
    }

    /**
     * Shows the account section: the signed-in account's email, gamertag and Minecraft name, and the
     * friends list. It is a Compose screen, so it replaces the fragment container with a ComposeView
     * built from the account store.
     */
    private void showAccount() {
        if (accountCompose == null) {
            accountCompose = new androidx.compose.ui.platform.ComposeView(this);
            accountCompose.setViewCompositionStrategy(
                    androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        }
        android.view.ViewGroup container = findViewById(R.id.customize_container);
        container.removeAllViews();
        container.addView(accountCompose,
                new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        accountState = org.chimeramc.client.ui.compose.account.AccountSectionHost.state(
                buildAccountState());
        org.chimeramc.client.ui.compose.account.AccountSectionHost.setContent(
                accountCompose,
                accountState,
                this::openXboxFriends,
                () -> startActivity(new Intent(this, AccountsActivity.class)));
        loadFriends();
        styleTabs(TAB_CONTROLLER, false);
        styleTabs(TAB_ACCOUNT, true);
    }

    /** Reads the active account into the section's data; friends start in the loading state. */
    private org.chimeramc.client.ui.compose.account.AccountSectionState buildAccountState() {
        activeAccount = null;
        try {
            for (org.chimeramc.client.core.auth.MsftAccountStore.MsftAccount account
                    : org.chimeramc.client.core.auth.MsftAccountStore.list(this)) {
                if (account.active) {
                    activeAccount = account;
                    return new org.chimeramc.client.ui.compose.account.AccountSectionState(
                            true,
                            account.msUserId,
                            account.xboxGamertag,
                            account.minecraftUsername,
                            account.xuid,
                            account.xboxAvatarUrl,
                            org.chimeramc.client.ui.compose.account.AccountSectionState.FriendsStatus.LOADING,
                            java.util.Collections.emptyList(),
                            "");
                }
            }
        } catch (Throwable t) {
            AppLog.warnOnce("GlowberryClient", "CustomizeActivity:account",
                    "Could not read the account for the account section", t);
        }
        return org.chimeramc.client.ui.compose.account.AccountSectionState.signedOut();
    }

    /**
     * Reads the active account's Xbox Live friends from PeopleHub.
     *
     * <p>The XSTS token is minted off the UI thread (it can refresh over the network), then the
     * friends read runs on the client's own thread; the result is pushed into the account state, so
     * the section moves from "Reading…" to the list, an empty list, or a named error.
     */
    private void loadFriends() {
        if (activeAccount == null || accountState == null) return;
        final org.chimeramc.client.core.auth.MsftAccountStore.MsftAccount account =
                activeAccount;
        org.chimeramc.client.util.AppExecutors.run(
                org.chimeramc.client.util.AppExecutors.Kind.NETWORK, "xbox-friends-token", () -> {
            String[] xsts = org.chimeramc.client.core.auth.MsftAuthManager
                    .xboxLiveXsts(account);
            if (xsts == null) {
                runOnUiThread(() -> setFriendsState(
                        org.chimeramc.client.ui.compose.account.AccountSectionState.FriendsStatus.ERROR,
                        java.util.Collections.emptyList(),
                        getString(R.string.account_friends_error)));
                return;
            }
            friendsClient.fetch(account.xuid, xsts[0], xsts[1], (friends, error) ->
                    runOnUiThread(() -> setFriendsState(
                            error == null
                                    ? org.chimeramc.client.ui.compose.account.AccountSectionState.FriendsStatus.LOADED
                                    : org.chimeramc.client.ui.compose.account.AccountSectionState.FriendsStatus.ERROR,
                            friends, error)));
        });
    }

    /** Pushes a friends result into the Compose state. */
    private void setFriendsState(
            org.chimeramc.client.ui.compose.account.AccountSectionState.FriendsStatus status,
            java.util.List<org.chimeramc.client.core.xbox.XboxPeopleHub.Friend> friends,
            String error) {
        if (accountState == null || isFinishing() || isDestroyed()) return;
        accountState.setValue(accountState.getValue().withFriends(status, friends, error));
    }

    /** Opens the account's Xbox friends page, which is where friends are managed for now. */
    private void openXboxFriends() {
        org.chimeramc.client.util.UrlOpener.open(this, "https://www.xbox.com/play/user");
    }

    private void replace(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.customize_container, fragment, tag)
                .commitAllowingStateLoss();
    }

    private void styleTabs(boolean controllerActive) {
        styleTabs(controllerActive ? TAB_CONTROLLER : TAB_SKINS, controllerActive);
    }

    /** Styles the chips for a selected tab id, so three tabs cannot disagree. */
    private void styleTabs(int selectedTab, boolean ignored) {
        styleTab(controllerTab, selectedTab == TAB_CONTROLLER);
        styleTab(skinsTab, selectedTab == TAB_SKINS);
        styleTab(accountTab, selectedTab == TAB_ACCOUNT);
    }

    private void styleTab(TextView tab, boolean active) {
        if (tab == null) return;
        if (active) {
            tab.setTextColor(accent != 0 ? accent : getColor(R.color.on_surface));
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            tab.setTextColor(getColor(R.color.text_secondary));
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.NORMAL);
        }
    }

    private Fragment currentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.customize_container);
    }

    // The controller illustration highlights the physically pressed button, so this host
    // keeps its raw presses and declines bumper-driven tab switching while it is showing.
    @Override
    protected boolean shouldHandleNavKeys() {
        Fragment current = currentFragment();
        return !(current instanceof ControllerSettingsFragment);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Fragment current = currentFragment();
        if (current instanceof ControllerSettingsFragment) {
            ((ControllerSettingsFragment) current).handleHardwareKey(keyCode, true);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Fragment current = currentFragment();
        if (current instanceof ControllerSettingsFragment) {
            ((ControllerSettingsFragment) current).handleHardwareKey(keyCode, false);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        Fragment current = currentFragment();
        if (current instanceof ControllerSettingsFragment) {
            ((ControllerSettingsFragment) current).handleHardwareMotion(event);
        }
        return super.onGenericMotionEvent(event);
    }
}
