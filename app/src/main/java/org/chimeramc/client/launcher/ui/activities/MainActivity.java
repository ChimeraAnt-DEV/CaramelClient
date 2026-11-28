package org.chimeramc.client.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.compose.runtime.MutableState;
import androidx.compose.ui.platform.ComposeView;
import androidx.compose.ui.platform.ViewCompositionStrategy;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.core.changelog.ChangelogFeed;
import org.chimeramc.client.core.content.ContentImporter;
import org.chimeramc.client.core.curseforge.CurseForgeBrowser;
import org.chimeramc.client.core.cosmetics.CosmeticCatalog;
import org.chimeramc.client.core.cosmetics.CosmeticStore;
import org.chimeramc.client.core.cosmetics.CaramelUserDirectory;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.auth.MsftAuthManager;
import org.chimeramc.client.core.content.ContentManager;
import org.chimeramc.client.core.content.FeaturedServers;
import org.chimeramc.client.core.content.RakNetPing;
import org.chimeramc.client.core.content.ScreenshotItem;
import org.chimeramc.client.core.content.ServerFavouritesStore;
import org.chimeramc.client.core.content.ServerItem;
import org.chimeramc.client.core.content.ServerListRows;
import org.chimeramc.client.core.content.ServerManager;
import org.chimeramc.client.core.content.ServerPongInfo;
import org.chimeramc.client.core.xbox.XboxFriendRequestClient;
import org.chimeramc.client.core.xbox.XboxFriendsClient;
import org.chimeramc.client.core.xbox.XboxPeopleHub;
import org.chimeramc.client.core.xbox.XboxSocial;
import org.chimeramc.client.core.xbox.CaramelIdentity;
import org.chimeramc.client.core.minecraft.InstanceOptionsStore;
import org.chimeramc.client.core.minecraft.PerfFeatureCatalog;
import org.chimeramc.client.core.minecraft.MinecraftLauncher;
import org.chimeramc.client.core.mods.Mod;
import org.chimeramc.client.core.mods.ModManager;
import org.chimeramc.client.core.mods.ModRowPresentation;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.launcher.util.LauncherSettingsController;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.launcher.core.mods.ModsFolderListing;
import org.chimeramc.client.launcher.settings.OptionsText;
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind;
import org.chimeramc.client.ui.compose.caramel.CaramelFileEntry;
import org.chimeramc.client.ui.compose.caramel.CaramelFriendInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelFriendRelation;
import org.chimeramc.client.ui.compose.caramel.CaramelFoundUser;
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState;
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelCurseForgeItem;
import org.chimeramc.client.ui.compose.caramel.CaramelNewsInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelScreenshotInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo;
import org.chimeramc.client.ui.compose.caramel.CaramelVersion;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelLauncherHost;
import org.chimeramc.client.ui.compose.caramel.launcher.CaramelReleaseNotesLoader;
import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.LocalFileOpener;
import org.chimeramc.client.util.UrlOpener;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The launcher's main screen: the Caramel UI pack's Compose launcher, hosted in a {@link ComposeView}.
 *
 * <p>This activity owns the state and the actions. It builds a {@link CaramelLauncherState} from the
 * live managers (versions, account, mods, servers, cosmetics, changelog, screenshots) and hands it
 * to the Kotlin host, which renders the pack's frame, tabs and modals. Every button on screen calls
 * back into a real launcher action here; nothing is a mock, and a list with no data shows its empty
 * state rather than a fabricated row.
 *
 * <p>The activity draws no chrome of its own. {@link BaseActivity} keeps the immersive full-screen
 * plumbing, and the Compose surface fills what is left.
 */
public class MainActivity extends BaseActivity implements CaramelLauncherHost.Actions {

    private static final String TAG = "MainActivity";

    /**
     * The tab to open on, by {@link org.chimeramc.client.ui.compose.caramel.CaramelTab} name.
     *
     * <p>Set when another screen hands the user to a specific tab — the settings screen's changelog
     * row lands on News. An unknown or absent name leaves the launcher on Home.
     */
    public static final String EXTRA_INITIAL_TAB = "main_initial_tab";

    private MutableState<CaramelLauncherState> launcherState;
    private VersionManager versionManager;
    private MinecraftLauncher minecraftLauncher;
    private ContentManager contentManager;
    private CosmeticStore cosmeticStore;
    private XboxFriendsClient friendsClient;
    private XboxFriendRequestClient friendRequestClient;

    /** The settings panel's store; the launcher's gear renders it. */
    private LauncherSettingsController settingsController;

    /** The account id whose friends are already loaded, so a rebuild does not refetch. */
    private String friendsAccountId = null;

    /**
     * Whether the last friends read for [friendsAccountId] finished without an error.
     *
     * <p>The account id alone is not enough to decide whether to refetch: the first read after a
     * sign-in often runs before the device has a usable network or a freshly renewed XSTS token, so
     * it fails and writes an error line. Without this flag the failed attempt still marked the
     * account as "loaded", and the friends list stayed on its error line for the rest of the
     * session — the "friends never sync" report. A read that failed (or never ran) is retried the
     * next time the launcher comes to the foreground; a successful one is not repeated.
     */
    private boolean friendsLoaded = false;

    /**
     * True while a friends read is in flight, so the many {@code refreshState()} callers do not
     * each start their own fetch. The retry-on-failure path below relies on this: without it, every
     * rebuild would stack another PeopleHub call on the executor.
     */
    private boolean friendsLoading = false;

    /** The user's saved servers, kept so a re-measure can rebuild the rows without a re-scan. */
    private List<ServerItem> savedServers = new ArrayList<>();

    /** The last measured status per {@code address:port}, empty until the first ping answers. */
    private final java.util.Map<String, ServerListRows.Status> serverStatuses =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** The starred server keys, read once and updated as the user stars and unstars. */
    private java.util.Set<String> serverFavourites = new java.util.HashSet<>();

    /** True while a ping sweep is in flight, so a scan that re-fires does not start a second one. */
    private boolean serverPingRunning = false;

    /** Rebuilds the Servers tab's rows from the saved servers, the defaults and the measurements. */
    private void publishServerRows() {
        List<ServerListRows.Row> rows = ServerListRows.build(savedServers, serverFavourites, serverStatuses);
        List<CaramelServerInfo> out = new ArrayList<>();
        for (ServerListRows.Row r : rows) {
            out.add(new CaramelServerInfo(r.name, r.address, r.port, r.online, r.ping, r.version,
                    r.favourite, r.iconKey, r.featured, r.checked));
        }
        mutateState(s -> s.setServers(out));
    }

    /**
     * Measures every listed server with a RakNet unconnected ping and republishes the rows.
     *
     * <p>The ping runs on the network executor, one target at a time, and a server that does not
     * answer keeps its unknown status rather than a fabricated zero. The rows are rebuilt once at
     * the end so the list does not flicker through a half-measured state.
     */
    private void refreshServerStatuses() {
        java.util.Map<String, Integer> targets = ServerListRows.pingTargets(savedServers);
        if (targets.isEmpty() || serverPingRunning) return;
        serverPingRunning = true;
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-server-ping", () -> {
            try {
                for (java.util.Map.Entry<String, Integer> target : targets.entrySet()) {
                    String key = target.getKey();
                    int port = target.getValue();
                    String host = key.substring(0, key.lastIndexOf(':'));
                    long started = System.currentTimeMillis();
                    ServerPongInfo pong = RakNetPing.ping(host, port, 2500);
                    if (pong == null || pong.parseError) {
                        // A single dropped datagram reads as "offline" for a server that is merely
                        // busy, which is what made healthy servers look unreachable. One retry with
                        // a longer window turns a lost packet into a second chance, not a verdict.
                        pong = RakNetPing.ping(host, port, 4000);
                    }
                    if (pong == null || pong.parseError) {
                        serverStatuses.put(key, ServerListRows.Status.offline());
                        continue;
                    }
                    int ping = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started);
                    serverStatuses.put(key, new ServerListRows.Status(pong.playersOnline, ping, pong.gameVersion));
                }
            } finally {
                serverPingRunning = false;
            }
            runOnUiThread(this::publishServerRows);
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minecraftLauncher = new MinecraftLauncher(this);
        settingsController = new LauncherSettingsController(this);
        launcherState = CaramelLauncherHost.state();
        serverFavourites = ServerFavouritesStore.load(this);
        applyInitialTab(getIntent());
        setContentView(buildComposeHost());
        initVersionManager();
        initContent();
        loadReleaseNotes();
        refreshState();
        announcePendingLaunchFailure();
    }

    /**
     * Reports a launch that died inside the game activity, once.
     *
     * <p>{@code MinecraftActivity} runs the game in its own process and tears the launcher down to
     * restart it, so a failure there cannot show a dialog at the time — it writes a report
     * ({@code LastLaunchFailure}, {@code announced=false}) for the relaunched launcher to show. The
     * writer existed but nothing ever read it back, so a crash inside the game left the user with the
     * app reappearing and no explanation. This is the missing reader: it names the classified cause
     * on the launcher's own toast and clears the report so it is announced exactly once.
     */
    private void announcePendingLaunchFailure() {
        org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure.Report report =
                org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure.consumePending(this);
        if (report == null) return;
        org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis.Cause cause = report.cause();
        toastWarning(getString(org.chimeramc.client.launcher.core.minecraft
                .LaunchErrorDiagnosis.messageRes(cause)));
    }

    /** Selects the tab named by [EXTRA_INITIAL_TAB], when it names a real one. */
    private void applyInitialTab(Intent intent) {
        if (intent == null) return;
        String name = intent.getStringExtra(EXTRA_INITIAL_TAB);
        if (name == null || name.isEmpty()) return;
        for (org.chimeramc.client.ui.compose.caramel.CaramelTab tab
                : org.chimeramc.client.ui.compose.caramel.CaramelTab.values()) {
            if (tab.name().equalsIgnoreCase(name)) {
                mutateState(s -> s.setTab(tab));
                return;
            }
        }
    }

    private ComposeView buildComposeHost() {
        ComposeView view = new ComposeView(this);
        view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool.INSTANCE);
        CaramelLauncherHost.setContent(view, launcherState, this, settingsController);
        return view;
    }

    private void initVersionManager() {
        VersionManager.initializeAsync(this, manager -> {
            versionManager = manager;
            refreshContentDirectories();
            refreshState();
        });
    }

    /**
     * Wires the content manager to the selected instance's game data directories and observes the
     * server and screenshot scans, so the Servers tab and the Screenshots modal draw real rows.
     */
    private void initContent() {
        contentManager = ContentManager.getInstance(this);
        contentManager.getServersLiveData().observe(this, servers -> {
            savedServers = servers == null ? new ArrayList<>() : new ArrayList<>(servers);
            publishServerRows();
            refreshServerStatuses();
        });
        contentManager.getScreenshotsLiveData().observe(this, shots -> {
            List<CaramelScreenshotInfo> rows = new ArrayList<>();
            if (shots != null) {
                for (ScreenshotItem s : shots) {
                    rows.add(new CaramelScreenshotInfo(s.name, s.file.getAbsolutePath()));
                }
            }
            mutateState(s -> s.setScreenshots(rows));
        });
    }

    /**
     * Loads the signed-in account's real Xbox Live friends.
     *
     * <p>PeopleHub is read with the account's own {@code http://xboxlive.com} XSTS token, which the
     * Bedrock auth chain already mints — no extra consent. The fetch runs on its own executor and
     * reports into the state, so the Friends card and modal draw real rows. A failure (no token, no
     * network, a PeopleHub error) becomes a status line rather than an empty list with no reason.
     */
    private void loadFriends() {
        MsftAccountStore.MsftAccount active = activeAccount();
        if (active == null || active.xuid == null || active.xuid.isEmpty()) {
            mutateState(s -> {
                s.setFriends(new ArrayList<>());
                s.setFriendsStatus("");
            });
            return;
        }
        if (friendsClient == null) friendsClient = new XboxFriendsClient();
        friendsLoading = true;
        mutateState(s -> s.setFriendsStatus(getString(R.string.caramel_friends_loading)));
        String xuid = active.xuid;
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-friends", () -> {
            String[] xsts = MsftAuthManager.xboxLiveXsts(active);
            if (xsts == null) {
                runOnUiThread(() -> {
                    friendsLoading = false;
                    // Left "not loaded" on purpose: the token was not ready, so the next resume
                    // retries rather than leaving the account stuck on the unavailable line.
                    friendsLoaded = false;
                    mutateState(s -> {
                        s.setFriends(new ArrayList<>());
                        s.setFriendsStatus(getString(R.string.caramel_friends_unavailable));
                    });
                });
                return;
            }
            friendsClient.fetchGraph(xuid, xsts[0], xsts[1], (graph, error) -> runOnUiThread(() -> {
                List<CaramelFriendInfo> rows = new ArrayList<>();
                if (graph != null) {
                    // Requests first: a request that is not at the top of the list is a request the
                    // user never sees, which is the state this list was in before the split.
                    addFriendRows(rows, graph.incoming, CaramelFriendRelation.REQUEST_RECEIVED);
                    addFriendRows(rows, graph.friends, CaramelFriendRelation.FRIEND);
                    addFriendRows(rows, graph.outgoing, CaramelFriendRelation.REQUEST_SENT);
                }
                friendsLoading = false;
                // A read that came back with a reason did not succeed, so it is retried on the next
                // resume; only a clean read (even an empty one) marks the account as loaded. A
                // follow-read failure still returns the friend list, so it counts as loaded with
                // its reason on screen rather than looping the whole read.
                friendsLoaded = graph != null;
                mutateState(s -> {
                    s.setFriends(rows);
                    s.setFriendsStatus(error == null ? "" : error);
                });
            }));
        });
    }

    /** Adds one class of PeopleHub row, in the order the read returned it. */
    private static void addFriendRows(List<CaramelFriendInfo> rows, List<XboxPeopleHub.Friend> people,
            CaramelFriendRelation relation) {
        if (people == null) return;
        for (XboxPeopleHub.Friend f : people) {
            rows.add(new CaramelFriendInfo(
                    f.xuid, f.gamertag, f.displayPicRaw, f.isOnline(), f.presenceText,
                    CaramelUserDirectory.isCaramelUser(f.xuid), relation));
        }
    }

    /** Points the content manager at the selected instance's folders, then rescans. */
    private void refreshContentDirectories() {
        if (contentManager == null || versionManager == null) return;
        File gameDataDir = activeGameDataDir();
        if (gameDataDir == null) return;
        contentManager.setStorageDirectories(
                new File(gameDataDir, "minecraftWorlds"),
                new File(gameDataDir, "resource_packs"),
                new File(gameDataDir, "behavior_packs"),
                new File(gameDataDir, "skin_packs"),
                new File(gameDataDir, "Screenshots"),
                new File(gameDataDir, "minecraftpe"));
    }

    /** The game data root the selected instance plays from, or null when none is selected. */
    private File activeGameDataDir() {
        GameVersion selected = selectedVersion();
        if (selected == null) return null;
        FeatureSettings.StorageType type = LauncherStorage.readSavedContentStorageType(this);
        return LauncherStorage.getActiveGameDataDir(
                this, selected.getStorageProfileId(), selected.versionIsolation, type);
    }

    /** Rebuilds the launcher state from the live managers and recomposes. */
    private void refreshState() {
        CaramelLauncherState s = launcherState.getValue();
        GameVersion selected = selectedVersion();
        s.setVersions(buildVersions());
        if (selected != null) {
            s.setVersion(new CaramelVersion(versionId(selected), isBeta(selected)));
        } else if (s.getVersion() == null && !s.getVersions().isEmpty()) {
            s.setVersion(s.getVersions().get(0));
        }
        s.setInstanceName(selected != null && selected.displayName != null ? selected.displayName : "");
        s.setInstanceEditable(selected != null && !selected.isInstalled);
        MsftAccountStore.MsftAccount active = activeAccount();
        s.setSignedIn(active != null);
        s.setAccountName(active != null ? displayName(active) : "");
        s.setAccountAvatarUrl(active != null && active.xboxAvatarUrl != null ? active.xboxAvatarUrl : "");
        // Publish the identity the Caramel-only protocols announce, so a peer who hears us can
        // badge the account by its xuid rather than by a device name.
        CaramelIdentity.set(active != null ? active.xuid : null,
                active != null ? active.xboxGamertag : null);
        String accountId = active != null ? active.id : null;
        boolean accountChanged = accountId == null
                ? friendsAccountId != null
                : !accountId.equals(friendsAccountId);
        if (accountChanged) {
            friendsAccountId = accountId;
            friendsLoaded = false;
            s.setFriends(new ArrayList<>());
            s.setFriendsStatus("");
        }
        // A failed read (or one that never ran because the token was not ready yet) is retried on
        // the next foreground; a successful one is not, so a rebuild does not refetch every resume.
        if (accountId != null && !friendsLoaded && !friendsLoading) {
            loadFriends();
        }
        // The count the title bar shows is the number of instances the launcher knows about, not a
        // mid-game flag. The old value was "is the game running", which is false almost always, so
        // the title bar read "0 instances" for a user with a full list of them.
        s.setInstancesCount(s.getVersions().size());
        s.setLaunchEnabled(selected != null);
        s.setMods(buildMods());
        s.setCosmetics(buildCosmetics());
        s.setNews(buildNews());
        // The instance-level flags the Advanced tab and the performance tab drive, read straight
        // from the selected version so a change made here is what the tab shows.
        s.setInstanceShaderCompat(selected != null && selected.shaderCompatEnabled);
        // Gate the Vibrant Visuals option on what the device can actually render, so a tablet with
        // no Vulkan or GLES 3.2 driver never sees a mode the game would silently ignore.
        s.setDeferredRenderSupported(
                org.chimeramc.client.core.minecraft.DeferredRenderProbe.supported(this));
        s.setInstanceIsolation(selected != null && selected.versionIsolation);
        s.setInstanceLaunchVertical(selected != null && selected.launchVertically);
        s.setInstancePackChanger(selected != null && selected.inGamePackChangerEnabled);
        s.setPerfValues(readPerfValues(selected));
        launcherState.setValue(s);
    }

    private List<CaramelVersion> buildVersions() {
        List<CaramelVersion> out = new ArrayList<>();
        if (versionManager == null) return out;
        List<GameVersion> installed = versionManager.getInstalledVersions();
        if (installed != null) {
            for (GameVersion v : installed) out.add(new CaramelVersion(versionId(v), isBeta(v)));
        }
        List<GameVersion> custom = versionManager.getCustomVersions();
        if (custom != null) {
            for (GameVersion v : custom) out.add(new CaramelVersion(versionId(v), isBeta(v)));
        }
        return out;
    }

    private static String versionId(GameVersion v) {
        if (v.versionCode != null && !v.versionCode.isEmpty()) return v.versionCode;
        return v.directoryName != null ? v.directoryName : "";
    }

    private static boolean isBeta(GameVersion v) {
        return versionId(v).toLowerCase(Locale.ROOT).contains("beta");
    }

    private List<CaramelModInfo> buildMods() {
        List<CaramelModInfo> out = new ArrayList<>();
        try {
            for (Mod m : ModManager.getInstance().getMods()) {
                ModRowPresentation.Kind kind = ModRowPresentation.kindOf(m);
                out.add(new CaramelModInfo(
                        m.getDisplayName() != null ? m.getDisplayName() : m.getFileName(),
                        m.getAuthor() != null ? m.getAuthor() : "",
                        m.getVersion() != null ? m.getVersion() : "",
                        m.getDescription() != null ? m.getDescription() : "",
                        m.isEnabled(),
                        kind.label,
                        ModRowPresentation.isRuntimeWired(kind)));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildMods", "Could not read the mod list", t);
        }
        return out;
    }

    /**
     * Builds the cosmetic tiles from the real catalogue.
     *
     * <p>Every catalogue entry is drawn and the equipped one is flagged from the store. The client
     * has no cosmetic-ownership model, so every catalogue entry is treated as owned rather than
     * inventing a lock the client cannot honour.
     */
    private List<CaramelCosmeticInfo> buildCosmetics() {
        List<CaramelCosmeticInfo> out = new ArrayList<>();
        try {
            if (cosmeticStore == null) cosmeticStore = new CosmeticStore(this);
            String equippedCape = cosmeticStore.getEquippedCapeId();
            String equippedAccessory = cosmeticStore.getEquippedAccessoryId();
            String equippedPet = cosmeticStore.getEquippedPetId();
            java.util.Set<String> favourites = cosmeticStore.getFavourites();
            for (CosmeticCatalog.Cape c : CosmeticCatalog.capes()) {
                out.add(new CaramelCosmeticInfo(c.id, c.name, CaramelCosmeticKind.Cape,
                        c.id.equals(equippedCape), true, favourites.contains(c.id)));
            }
            for (CosmeticCatalog.Accessory a : CosmeticCatalog.accessories()) {
                CaramelCosmeticKind kind = a.kind != null && a.kind.followsHead()
                        ? CaramelCosmeticKind.Hat : CaramelCosmeticKind.Wings;
                out.add(new CaramelCosmeticInfo(a.id, a.name, kind,
                        a.id.equals(equippedAccessory), true, favourites.contains(a.id)));
            }
            for (CosmeticCatalog.Pet p : CosmeticCatalog.pets()) {
                out.add(new CaramelCosmeticInfo(p.id, p.name, CaramelCosmeticKind.Pet,
                        p.id.equals(equippedPet), true, favourites.contains(p.id)));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildCosmetics", "Could not read the cosmetic catalogue", t);
        }
        return out;
    }

    /** Reads the bundled changelog feed into news rows; an unreadable feed yields no rows. */
    private List<CaramelNewsInfo> buildNews() {
        List<CaramelNewsInfo> out = new ArrayList<>();
        try (InputStream in = getAssets().open("launcher/changelog_feed.json")) {
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int read;
            while ((read = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
            ChangelogFeed feed = ChangelogFeed.parse(sb.toString());
            ChangelogFeed.Section newest = feed.newest();
            if (newest != null && newest.headline != null && !newest.headline.isEmpty()) {
                out.add(new CaramelNewsInfo(newest.headline, joinItems(newest.items),
                        newest.version, true, "", "", R.drawable.news_banner_beta3));
            }
            ChangelogFeed.Section upcoming = feed.upcoming();
            if (upcoming != null && upcoming.headline != null && !upcoming.headline.isEmpty()) {
                out.add(new CaramelNewsInfo(upcoming.headline, joinItems(upcoming.items),
                        upcoming.version, false, "", "", R.drawable.news_banner_berryapi));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "buildNews", "Could not read the changelog feed", t);
        }
        // Bundled announcements that are not part of the changelog feed.
        out.addAll(org.chimeramc.client.core.changelog.BundledNews.posts(this));
        return out;
    }

    /** Reads the bundled release notes into the shape the Release notes popup draws. */
    private void loadReleaseNotes() {
        String json = null;
        try (InputStream in = getAssets().open("launcher/changelog.json")) {
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int read;
            while ((read = in.read(buf)) > 0) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
            json = sb.toString();
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "loadReleaseNotes", "Could not read the bundled release notes", t);
        }
        CaramelLauncherState s = launcherState.getValue();
        s.setReleaseNotes(CaramelReleaseNotesLoader.fromJson(json, BuildConfig.VERSION_NAME));
    }

    private static String joinItems(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item == null || item.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" \u00b7 ");
            sb.append(item);
        }
        return sb.toString();
    }

    private MsftAccountStore.MsftAccount activeAccount() {
        for (MsftAccountStore.MsftAccount a : MsftAccountStore.list(this)) {
            if (a.active) return a;
        }
        return null;
    }

    private static String displayName(MsftAccountStore.MsftAccount a) {
        if (a.minecraftUsername != null && !a.minecraftUsername.isEmpty()) return a.minecraftUsername;
        if (a.xboxGamertag != null && !a.xboxGamertag.isEmpty()) return a.xboxGamertag;
        return "";
    }

    private GameVersion selectedVersion() {
        return versionManager != null ? versionManager.getSelectedVersion() : null;
    }

    private void toast(int resId) {
        toast(getString(resId));
    }

    /**
     * Raises a notice on the launcher's own toast.
     *
     * <p>This used to be an {@link android.widget.Toast}, which draws a square system bubble outside
     * the launcher's visual language and ignores the reduce-motion setting. Routing through
     * {@link CaramelNotice} means every message wears the same surface, animates, and honours the
     * user's preferences. A message raised before the launcher has mounted is dropped, which is
     * correct: there is nothing to draw it over yet.
     */
    private void toast(String message) {
        toast(message, false);
    }

    /** A notice that reports a failure, drawn with the toast's warning rail and glyph. */
    private void toastWarning(String message) {
        toast(message, true);
    }

    private void toast(String message, boolean warning) {
        if (message == null || message.isEmpty()) return;
        mutateState(s -> s.setNotice(
                new org.chimeramc.client.ui.compose.caramel.CaramelNotice(message, warning)));
    }

    /**
     * The launcher's receiver for notices raised by screens outside the Compose tree.
     *
     * <p>In-game overlays, the logcat dialog and the screenshot share sheet cannot reach the Compose
     * toast directly and drew a raw system {@link android.widget.Toast} instead, so the same app
     * showed two notice styles. They now post to {@link CaramelNoticeBus}, and this sink lands the
     * message on the one Caramel toast. It is marshalled to the UI thread because a poster may be on
     * a game or network thread.
     */
    private final org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.Sink noticeSink =
            (text, warning) -> runOnUiThread(() -> toast(text, warning));

    @Override
    protected void onStart() {
        super.onStart();
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.attach(noticeSink);
    }

    @Override
    protected void onStop() {
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.detach(noticeSink);
        super.onStop();
    }

    /** Applies a change to the Compose state on the UI thread and recomposes. */
    private void mutateState(StateMutation mutation) {
        CaramelLauncherState s = launcherState.getValue();
        mutation.apply(s);
        launcherState.setValue(s);
    }

    private interface StateMutation {
        void apply(CaramelLauncherState state);
    }

    // --- CaramelLauncherHost.Actions ------------------------------------------------------------

    @Override
    public void launch() {
        GameVersion selected = selectedVersion();
        if (selected == null) return;
        mutateState(s -> s.setLaunching(true));
        minecraftLauncher.launch(getIntent(), selected, new MinecraftLauncher.LaunchCallback() {
            @Override
            public void onLaunchStarted() {
                mutateState(s -> s.setLaunching(false));
            }

            @Override
            public void onLaunchFailed(Throwable e) {
                mutateState(s -> s.setLaunching(false));
                AppLog.warnOnce(TAG, "launch", "The game did not start", e);
                // The failure happened before the game activity existed, so there is no launch log
                // for the user to read and the Play button simply went back to its idle state with
                // no explanation. Name the classified cause on the launcher's own toast, the same
                // surface every other message uses, so the user is told why nothing happened.
                org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis.Cause cause =
                        org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis.classify(e);
                toastWarning(getString(org.chimeramc.client.launcher.core.minecraft
                        .LaunchErrorDiagnosis.messageRes(cause)));
            }
        });
    }

    @Override
    public void selectVersion(String id) {
        if (versionManager == null) return;
        GameVersion match = findVersion(id);
        if (match == null) return;
        versionManager.selectVersion(match);
        refreshContentDirectories();
        refreshState();
    }

    private GameVersion findVersion(String id) {
        if (versionManager == null) return null;
        List<GameVersion> all = new ArrayList<>();
        if (versionManager.getInstalledVersions() != null) all.addAll(versionManager.getInstalledVersions());
        if (versionManager.getCustomVersions() != null) all.addAll(versionManager.getCustomVersions());
        for (GameVersion v : all) {
            if (versionId(v).equals(id)) return v;
        }
        return null;
    }

    @Override
    public void toggleMod(CaramelModInfo mod, boolean on) {
        try {
            ModManager.getInstance().setModEnabled(mod.getName(), on);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "toggleMod", "Could not toggle the mod", t);
        }
        refreshState();
    }

    @Override
    public void equipCosmetic(CaramelCosmeticInfo cosmetic) {
        try {
            if (cosmeticStore == null) cosmeticStore = new CosmeticStore(this);
            switch (cosmetic.getKind()) {
                case Cape -> cosmeticStore.setEquippedCape(cosmetic.getId());
                case Wings, Hat -> cosmeticStore.setEquippedAccessory(cosmetic.getId());
                case Pet -> cosmeticStore.setEquippedPet(cosmetic.getId());
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "equipCosmetic", "Could not save the equipped cosmetic", t);
        }
        refreshState();
    }

    @Override
    public void toggleFavourite(CaramelCosmeticInfo cosmetic) {
        try {
            if (cosmeticStore == null) cosmeticStore = new CosmeticStore(this);
            cosmeticStore.toggleFavourite(cosmetic.getId());
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "toggleFavourite", "Could not save the favourite", t);
        }
        refreshState();
    }

    @Override
    public void joinServer(CaramelServerInfo server) {
        // Bedrock has no launcher-side "join server" intent; the saved server reaches the game
        // through external_servers.txt, so the honest action is to launch the instance. A featured
        // server the user has not saved is written to that file first, so the game can see it.
        if (server != null && server.getFeatured()) {
            saveServerIfAbsent(server);
        }
        launch();
    }

    @Override
    public void addServer() {
        // Open the in-app editor rather than the folder: handing the directory to a file manager
        // did nothing on a device with none, and asked the user to hand-edit the game's file.
        mutateState(s -> {
            s.setServerDraftName("");
            s.setServerDraftAddress("");
            s.setServerDraftPort(String.valueOf(FeaturedServers.DEFAULT_PORT));
            s.setAddServerError("");
            s.setAddServerRunning(false);
            s.setAddServer(true);
        });
    }

    @Override
    public void submitAddServer() {
        CaramelLauncherState s = launcherState.getValue();
        String name = s.getServerDraftName().trim();
        String address = s.getServerDraftAddress().trim();
        String portText = s.getServerDraftPort().trim();
        if (name.isEmpty() || address.isEmpty()) {
            mutateState(x -> x.setAddServerError(getString(R.string.server_details_required)));
            return;
        }
        int port = FeaturedServers.DEFAULT_PORT;
        if (!portText.isEmpty()) {
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException e) {
                mutateState(x -> x.setAddServerError(getString(R.string.server_ip_required)));
                return;
            }
        }
        if (port <= 0 || port > 65535) port = FeaturedServers.DEFAULT_PORT;

        final int finalPort = port;
        final ServerItem item = new ServerItem(name, address, finalPort);
        mutateState(x -> {
            x.setAddServerRunning(true);
            x.setAddServerError("");
        });
        contentManager.addServer(item, new ContentManager.ContentOperationCallback() {
            @Override
            public void onSuccess(String message) {
                mutateState(x -> {
                    x.setAddServerRunning(false);
                    x.setAddServer(false);
                });
                toast(R.string.server_added);
            }

            @Override
            public void onError(String error) {
                mutateState(x -> {
                    x.setAddServerRunning(false);
                    x.setAddServerError(error);
                });
            }
        });
    }

    @Override
    public void resetAddServer() {
        mutateState(x -> {
            x.setServerDraftName("");
            x.setServerDraftAddress("");
            x.setServerDraftPort("");
            x.setAddServerError("");
            x.setAddServerRunning(false);
        });
    }

    @Override
    public void deleteServer(CaramelServerInfo server) {
        if (server == null || contentManager == null) return;
        ServerItem item = new ServerItem(server.getName(), server.getAddress(), server.getPort());
        contentManager.deleteServer(item, new ContentManager.ContentOperationCallback() {
            @Override
            public void onSuccess(String message) {
                serverStatuses.remove(ServerListRows.key(server.getAddress(), server.getPort()));
                toast(R.string.server_deleted);
            }

            @Override
            public void onError(String error) {
                toast(error);
            }
        });
    }

    @Override
    public void toggleServerFavourite(CaramelServerInfo server) {
        if (server == null) return;
        serverFavourites = ServerFavouritesStore.toggle(this, ServerListRows.key(server.getAddress(), server.getPort()));
        publishServerRows();
    }

    /** Writes a featured server into the game's file if it is not already saved, and ignores failure. */
    private void saveServerIfAbsent(CaramelServerInfo server) {
        GameVersion selected = selectedVersion();
        File dataDir = selected == null ? null : activeGameDataDir();
        if (dataDir == null) return;
        File file = new File(new File(dataDir, "minecraftpe"), "external_servers.txt");
        ServerManager.writeServerToFile(file, new ServerItem(server.getName(), server.getAddress(), server.getPort()));
    }

    @Override
    public void openModsFolder() {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.modsDir == null) return;
        // The folder is listed in-app: handing it to a file manager does nothing on a device that
        // has none, and the modal needs a way in regardless of what is installed.
        File root = selected.modsDir;
        root.mkdirs();
        showModsFolder(root, root);
        mutateState(s -> s.setModsFolderOpen(true));
    }

    @Override
    public void openModsFolderEntry(CaramelFileEntry entry) {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.modsDir == null || entry == null) return;
        File root = selected.modsDir;
        File target = new File(entry.getPath());
        if (entry.getDirectory()) {
            showModsFolder(root, target);
        } else if (!LocalFileOpener.openImage(this, target)) {
            // Not an image (a .so, an archive): there is nothing to preview, so open the folder it
            // lives in through a file manager and say so if that also fails.
            if (!LocalFileOpener.openFolder(this, target.getParentFile())) {
                toast(R.string.caramel_open_folder_failed);
            }
        }
    }

    @Override
    public void openModsFolderUp() {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.modsDir == null) return;
        File root = selected.modsDir;
        String path = launcherState.getValue().getModsFolderPath();
        File current = "/".equals(path) ? root : new File(root, path);
        File parent = current.getParentFile();
        if (parent == null) return;
        // Never step above the mods root: the browser is scoped to this instance.
        if (!parent.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            showModsFolder(root, root);
            return;
        }
        showModsFolder(root, parent);
    }

    /** Fills the browser's state from {@code folder}, keeping [root] as the visible boundary. */
    private void showModsFolder(File root, File folder) {
        String path = ModsFolderListing.relativePath(root, folder);
        List<CaramelFileEntry> rows = new ArrayList<>();
        for (ModsFolderListing.Entry entry : ModsFolderListing.list(folder)) {
            rows.add(new CaramelFileEntry(
                    entry.name,
                    entry.path,
                    entry.directory,
                    ModsFolderListing.formatSize(entry.sizeBytes)));
        }
        boolean canGoUp = !"/".equals(path);
        mutateState(s -> {
            s.setModsFolderPath(path);
            s.setModsFolderEntries(rows);
            s.setModsFolderCanGoUp(canGoUp);
        });
    }

    /**
     * Opens the selected instance's Screenshots folder in a file manager.
     *
     * <p>The modal's "Open folder" buttons used to call {@link #openScreenshots()}, which only
     * toggled the modal they were already in — a dead press. This opens the real folder the shots
     * were scanned from.
     */
    @Override
    public void openScreenshotsFolder() {
        GameVersion selected = selectedVersion();
        File dataDir = selected == null ? null : activeGameDataDir();
        if (dataDir == null) return;
        if (!LocalFileOpener.openFolder(this, new File(dataDir, "Screenshots"))) {
            toast(R.string.caramel_open_folder_failed);
        }
    }

    @Override
    public void openSettings() {
        // Settings is a sidebar tab now, not a separate activity: selecting it keeps the launcher
        // frame (backdrop, title bar, sidebar) around the panel, so it reads as a destination rather
        // than a window that covers the app. The full-screen SettingsActivity remains for the
        // manifest's own entry point but is no longer where the gear sends the user.
        mutateState(s -> s.setTab(org.chimeramc.client.ui.compose.caramel.CaramelTab.Settings));
    }

    /** Rebuilds the settings panel from the stores after the settings screen changed them. */
    private void refreshSettingsPanel() {
        if (settingsController != null) settingsController.refresh();
    }

    @Override
    public void openAccounts() {
        // Accounts are managed through the account modal; there is no separate screen.
    }

    @Override
    public void openScreenshots() {
        mutateState(s -> s.setScreenshotsModal(true));
    }

    @Override
    public void openScreenshot(CaramelScreenshotInfo screenshot) {
        LocalFileOpener.openImage(this, new File(screenshot.getPath()));
    }

    @Override
    public void deleteScreenshot(CaramelScreenshotInfo screenshot) {
        try {
            new File(screenshot.getPath()).delete();
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "deleteScreenshot", "Could not delete the screenshot", t);
        }
        if (contentManager != null) contentManager.refreshScreenshots();
        mutateState(s -> s.setSelectedScreenshot(null));
    }

    /**
     * Starts the Microsoft device-code sign-in.
     *
     * <p>The button used to call this and see nothing happen. Three things were wrong, and each one
     * on its own was enough to make the press look dead: the code was never shown for long enough to
     * act on, the browser was never opened, and every failure was written to the log and then
     * discarded, so the modal closed with no account, no code and no message. This runs the flow the
     * device-code path actually supports (the Bedrock title id is an MSA title, so the MSAL browser
     * flow cannot authenticate it) and reports each step into the modal.
     *
     * <p>The page itself opens in the launcher's own WebView rather than the system browser: the
     * user stays in the app to enter the code. [openDeviceCodePage] remains the deliberate fallback
     * to the system browser.
     */
    @Override
    public void startSignIn() {
        mutateState(s -> {
            s.setSignInRunning(true);
            s.setSignInError("");
            s.setDeviceCode("");
            s.setDeviceCodeUrl("");
            s.setSignInWebOpen(false);
            s.setBusyMessage(getString(R.string.caramel_sign_in_starting));
        });
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-sign-in", () -> {
            String failure = null;
            try {
                BedrockAuthManager manager = MsftAuthManager.loginWithDeviceCode(
                        code -> runOnUiThread(() -> {
                            // The code is stored and the in-app page opened, so the user returns to a
                            // modal that already carries the code and the pre-filled link.
                            mutateState(s -> {
                                s.setDeviceCode(code.getUserCode());
                                s.setDeviceCodeUrl(code.getDirectVerificationUri());
                                s.setSignInWebOpen(true);
                                s.setBusyMessage(getString(R.string.caramel_waiting_sign_in));
                            });
                        }),
                        org.chimeramc.client.core.auth.Cancellation.none(),
                        state -> { });
                MsftAccountStore.MsftAccount account = MsftAuthManager.saveAccountAndActivate(this, manager);
                runOnUiThread(() -> toast(getString(R.string.ms_login_success, displayName(account))));
            } catch (Throwable t) {
                failure = MsftAuthManager.describeError(t);
                AppLog.warnOnce(TAG, "startSignIn", "Microsoft sign-in did not complete", t);
            }
            final String error = failure;
            runOnUiThread(() -> {
                mutateState(s -> {
                    s.setBusyMessage("");
                    s.setSignInRunning(false);
                    s.setSignInError(error == null ? "" : error);
                    if (error == null) {
                        s.setDeviceCode("");
                        s.setDeviceCodeUrl("");
                        s.setSignInWebOpen(false);
                        s.setAddAccount(false);
                    }
                });
                if (error != null) toast(error);
                refreshState();
            });
        });
    }

    /**
     * The deliberate escape hatch: opens the verification page in the system browser.
     *
     * <p>The sign-in page normally renders in the launcher's own WebView. Some Microsoft sign-in
     * steps (a hardware-key prompt, an account picker that blocks a WebView) do not complete in an
     * embedded view, so the user can still hand this one page to their browser from the modal.
     */
    @Override
    public void openDeviceCodePage() {
        CaramelLauncherState s = launcherState.getValue();
        if (!UrlOpener.open(this, s.getDeviceCodeUrl())) {
            toast(R.string.caramel_device_code_open_failed);
        }
    }

    @Override
    public void copyDeviceCode() {
        String code = launcherState.getValue().getDeviceCode();
        if (code == null || code.isEmpty()) return;
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null) return;
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Microsoft device code", code));
            toast(R.string.ms_device_code_copied);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "copyDeviceCode", "Could not copy the device code", t);
        }
    }

    @Override
    public void signOut() {
        MsftAccountStore.MsftAccount active = activeAccount();
        if (active != null) MsftAccountStore.remove(this, active.id);
        CaramelIdentity.clear();
        mutateState(s -> {
            s.setAccountMenu(false);
            s.setAccountSettings(false);
        });
        refreshState();
    }

    /**
     * Resolves a typed gamertag against the real Xbox Live profile service.
     *
     * <p>The modal used to show a green "found" banner regardless of what was typed, because nothing
     * ever resolved the name. This does the real lookup with the account's XSTS token and reports one
     * of three honest outcomes: an account (with the Caramel badge only when its xuid was actually
     * heard on a Caramel-only protocol), "no such player", or a failure reason.
     */
    @Override
    public void searchFriend(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            mutateState(s -> {
                s.setFriendFound(null);
                s.setFriendFoundIsCaramel(false);
                s.setFriendSearchError("");
            });
            return;
        }
        MsftAccountStore.MsftAccount active = activeAccount();
        if (active == null || active.xuid == null || active.xuid.isEmpty()) {
            mutateState(s -> {
                s.setFriendFound(null);
                s.setFriendFoundIsCaramel(false);
                s.setFriendSearchError(getString(R.string.caramel_sign_in_to_add));
            });
            return;
        }
        if (friendRequestClient == null) friendRequestClient = new XboxFriendRequestClient();
        mutateState(s -> {
            s.setFriendSearchRunning(true);
            s.setFriendFound(null);
            s.setFriendFoundIsCaramel(false);
            s.setFriendSearchError("");
            s.setFriendRequestResult("");
        });
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-friend-search", () -> {
            String[] xsts = MsftAuthManager.xboxLiveXsts(active);
            if (xsts == null) {
                runOnUiThread(() -> mutateState(s -> {
                    s.setFriendSearchRunning(false);
                    s.setFriendSearchError(getString(R.string.caramel_search_failed));
                }));
                return;
            }
            friendRequestClient.search(xsts[0], xsts[1], trimmed, (user, error) -> runOnUiThread(() -> {
                if (user != null) {
                    CaramelFoundUser found = new CaramelFoundUser(
                            user.xuid, user.label(), user.avatarUrl);
                    boolean caramel = CaramelUserDirectory.isCaramelUser(user.xuid);
                    mutateState(s -> {
                        s.setFriendSearchRunning(false);
                        s.setFriendFound(found);
                        s.setFriendFoundIsCaramel(caramel);
                        s.setFriendSearchError("");
                    });
                } else {
                    String reason = error != null ? error : getString(R.string.caramel_player_not_found);
                    mutateState(s -> {
                        s.setFriendSearchRunning(false);
                        s.setFriendFound(null);
                        s.setFriendFoundIsCaramel(false);
                        s.setFriendSearchError(reason);
                    });
                }
            }));
        });
    }

    /** Sends the real Xbox Live follow request for the account the last lookup resolved. */
    @Override
    public void sendFriendRequest() {
        CaramelFoundUser found = launcherState.getValue().getFriendFound();
        if (found == null) return;
        MsftAccountStore.MsftAccount active = activeAccount();
        if (active == null || active.xuid == null || active.xuid.isEmpty()) {
            mutateState(s -> {
                s.setFriendRequestSucceeded(false);
                s.setFriendRequestResult(getString(R.string.caramel_sign_in_to_add));
            });
            return;
        }
        if (found.getXuid().equalsIgnoreCase(active.xuid)) {
            mutateState(s -> {
                s.setFriendRequestSucceeded(false);
                s.setFriendRequestResult(getString(R.string.caramel_friend_request_failed));
            });
            return;
        }
        if (friendRequestClient == null) friendRequestClient = new XboxFriendRequestClient();
        mutateState(s -> {
            s.setFriendRequestRunning(true);
            s.setFriendRequestResult("");
        });
        AppExecutors.run(AppExecutors.Kind.NETWORK, "caramel-friend-add", () -> {
            String[] xsts = MsftAuthManager.xboxLiveXsts(active);
            if (xsts == null) {
                runOnUiThread(() -> mutateState(s -> {
                    s.setFriendRequestRunning(false);
                    s.setFriendRequestSucceeded(false);
                    s.setFriendRequestResult(getString(R.string.caramel_friend_request_failed));
                }));
                return;
            }
            friendRequestClient.addFriend(xsts[0], xsts[1], found.getXuid(), (success, error) -> runOnUiThread(() -> {
                mutateState(s -> {
                    s.setFriendRequestRunning(false);
                    s.setFriendRequestSucceeded(success);
                    s.setFriendRequestResult(success
                            ? getString(R.string.caramel_friend_request_sent)
                            : (error != null ? error : getString(R.string.caramel_friend_request_failed)));
                });
                if (success) loadFriends();
            }));
        });
    }

    /** Clears the add-friend modal's transient state when it closes, so it reopens clean. */
    @Override
    public void resetFriendSearch() {
        mutateState(s -> {
            s.setFriendQuery("");
            s.setFriendFound(null);
            s.setFriendFoundIsCaramel(false);
            s.setFriendSearchError("");
            s.setFriendSearchRunning(false);
            s.setFriendRequestResult("");
            s.setFriendRequestSucceeded(false);
            s.setFriendRequestRunning(false);
        });
    }

    /**
     * Re-reads the account's Xbox Live friends on demand.
     *
     * <p>The automatic read runs once per account and is not repeated after a success, so without a
     * manual entry point a user whose first read failed (offline at sign-in, a token that was not
     * ready) had no way to make the list appear until the account changed. This is what the Friends
     * modal's refresh button calls: it forces one more read regardless of the loaded flag.
     */
    @Override
    public void refreshFriends() {
        if (friendsLoading) return;
        friendsLoaded = false;
        loadFriends();
    }

    @Override
    public void renameInstance(String name) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceDisplayName(selected, name);
        refreshState();
    }

    @Override
    public void repairInstance() {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        mutateState(s -> s.setBusyMessage(getString(R.string.caramel_busy_repair)));
        versionManager.repairLibsAsync(selected, new VersionManager.LibsRepairCallback() {
            @Override
            public void onRepairStarted() {
            }

            @Override
            public void onRepairProgress(int progress) {
            }

            @Override
            public void onRepairCompleted(boolean success) {
                runOnUiThread(() -> {
                    mutateState(s -> s.setBusyMessage(""));
                    toast(success ? R.string.repair_completed : R.string.repair_failed);
                });
            }

            @Override
            public void onRepairFailed(Throwable e) {
                runOnUiThread(() -> {
                    mutateState(s -> s.setBusyMessage(""));
                    AppLog.warnOnce(TAG, "repairInstance", "The instance repair failed", e);
                    toast(R.string.repair_failed);
                });
            }
        });
    }

    @Override
    public void deleteInstance() {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null || selected.isInstalled) return;
        versionManager.deleteCustomVersion(selected, new VersionManager.OnDeleteVersionCallback() {
            @Override
            public void onDeleteCompleted(boolean success) {
                runOnUiThread(() -> {
                    refreshState();
                    if (!success) toast(R.string.toast_delete_failed);
                });
            }

            @Override
            public void onDeleteFailed(Exception e) {
                runOnUiThread(() -> {
                    AppLog.warnOnce(TAG, "deleteInstance", "Could not delete the instance", e);
                    toast(R.string.toast_delete_failed);
                });
            }
        });
    }

    @Override
    public void openInstanceFolder() {
        GameVersion selected = selectedVersion();
        if (selected == null || selected.versionDir == null) return;
        LocalFileOpener.openFolder(this, selected.versionDir);
    }

    @Override
    public void setPerfFeature(String featureId, String value) {
        PerfFeatureCatalog.Feature feature = PerfFeatureCatalog.find(featureId);
        GameVersion selected = selectedVersion();
        if (feature == null || selected == null) return;
        Map<String, String> write = new HashMap<>();
        write.put(feature.key, value);
        applyPerfWrite(selected, write, feature);
    }

    @Override
    public void resetPerfFeatures() {
        GameVersion selected = selectedVersion();
        if (selected == null) return;
        Map<String, String> write = new LinkedHashMap<>();
        for (PerfFeatureCatalog.Feature feature : PerfFeatureCatalog.all()) {
            write.put(feature.key, feature.defaultValue);
        }
        applyPerfWrite(selected, write, null);
    }

    /**
     * Writes [values] into the instance's options file and republishes the tab's values.
     *
     * <p>The write runs on the disk pool; the state is updated from the written file rather than
     * from the request, so what the tab shows is what the game will read. A failure reports rather
     * than leaving the switch looking like it took effect.
     */
    private void applyPerfWrite(GameVersion version, Map<String, String> values,
                                PerfFeatureCatalog.Feature feature) {
        AppExecutors.run(AppExecutors.Kind.DISK, "caramel-perf-write", () -> {
            boolean ok = InstanceOptionsStore.write(this, version, values);
            String text = ok ? InstanceOptionsStore.read(this, version) : null;
            Map<String, String> readBack = new HashMap<>();
            if (text != null) {
                for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
                    String v = OptionsText.get(text, f.key);
                    if (v != null) readBack.put(f.id, v);
                }
            }
            String status = ok
                    ? getString(R.string.caramel_perf_applied, feature == null
                            ? getString(R.string.caramel_perf_all_features) : feature.name)
                    : getString(R.string.caramel_perf_write_failed);
            runOnUiThread(() -> mutateState(s -> {
                if (ok) s.setPerfValues(readBack);
                s.setPerfStatus(status);
            }));
        });
    }

    @Override
    public void setInstanceShaderCompat(boolean enabled) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceShaderCompatEnabled(selected, enabled);
        refreshState();
    }

    @Override
    public void setInstanceIsolation(boolean enabled) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceVersionIsolation(selected, enabled);
        refreshState();
    }

    @Override
    public void setInstanceLaunchVertical(boolean enabled) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceLaunchVertically(selected, enabled);
        refreshState();
    }

    @Override
    public void setInstancePackChanger(boolean enabled) {
        GameVersion selected = selectedVersion();
        if (selected == null || versionManager == null) return;
        versionManager.setInstanceInGamePackChangerEnabled(selected, enabled);
        refreshState();
    }

    /**
     * Reads the selected instance's performance values from its options file.
     *
     * <p>The values the tab draws are the file's own, so a setting the user changed in the game
     * shows up here rather than a launcher-side default. A missing key is simply absent from the
     * map, and the tab then draws the catalogue's own default.
     */
    private Map<String, String> readPerfValues(GameVersion version) {
        Map<String, String> values = new HashMap<>();
        if (version == null) return values;
        String text = InstanceOptionsStore.read(this, version);
        for (PerfFeatureCatalog.Feature f : PerfFeatureCatalog.all()) {
            String v = OptionsText.get(text, f.key);
            if (v != null) values.put(f.id, v);
        }
        return values;
    }

    @Override
    public void quit() {
        finishAffinity();
    }

    /**
     * The package picker for "Import instance".
     *
     * <p>Registered as a field so it exists before the activity is started, which is the only time
     * the activity-result API accepts a registration. The chooser asks for every MIME type rather
     * than a fixed one: an `.xapk` has no registered MIME type on most devices, and a narrower
     * filter hides the very files this exists to import.
     */
    private final ActivityResultLauncher<String[]> instancePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importInstanceFrom(uri);
            });

    @Override
    public void importInstance() {
        try {
            instancePicker.launch(new String[]{"*/*"});
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "import-picker", "The package picker could not be opened", t);
            toastWarning(getString(R.string.caramel_import_picker_unavailable));
        }
    }

    /** Copies the picked package into the cache, then hands it to the version importer. */
    private void importInstanceFrom(Uri uri) {
        if (versionManager == null) return;
        mutateState(s -> {
            s.setBusyMessage(getString(R.string.caramel_import_instance_progress, 0));
            s.setImportProgress(0);
        });
        AppExecutors.run(AppExecutors.Kind.DISK, "instance-import-copy", () -> {
            File copied = null;
            try {
                String name = queryDisplayName(uri);
                if (name == null || name.isEmpty()) name = "instance.apk";
                File dir = new File(getCacheDir(), "instance-import");
                if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("no cache directory");
                copied = new File(dir, name);
                try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(copied)) {
                    if (in == null) throw new java.io.IOException("the picked file could not be read");
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
                }
                File file = copied;
                versionManager.importGameFile(file, new VersionManager.VersionCallback() {
                    @Override
                    public void onSuccess(String versionId) {
                        runOnUiThread(() -> {
                            mutateState(s -> {
                                s.setBusyMessage("");
                                s.setImportProgress(-1);
                            });
                            toast(getString(R.string.caramel_import_instance_done, versionId));
                            refreshState();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            mutateState(s -> {
                                s.setBusyMessage("");
                                s.setImportProgress(-1);
                            });
                            toastWarning(getString(R.string.caramel_import_instance_failed, error));
                        });
                    }

                    @Override
                    public void onProgress(int percent) {
                        runOnUiThread(() -> mutateState(s -> {
                            s.setBusyMessage(getString(R.string.caramel_import_instance_progress, percent));
                            s.setImportProgress(percent);
                        }));
                    }
                });
            } catch (Throwable t) {
                AppLog.warnOnce(TAG, "instance-import", "Copying the picked package failed", t);
                runOnUiThread(() -> {
                    mutateState(s -> {
                        s.setBusyMessage("");
                        s.setImportProgress(-1);
                    });
                    toastWarning(getString(R.string.caramel_import_instance_failed,
                            t.getMessage() == null ? "" : t.getMessage()));
                });
            }
        });
    }

    /** The picked document's display name, or null when the provider does not report one. */
    @Nullable
    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "display-name", "The picker did not report a file name", t);
        }
        return null;
    }

    @Override
    public void searchCurseForge(String query, int classId) {
        mutateState(s -> {
            s.setCurseForgeQuery(query == null ? "" : query);
            s.setCurseForgeSection(org.chimeramc.client.ui.compose.caramel.CurseForgeSection
                    .Companion.forClassId(classId));
            s.setCurseForgeBusy(true);
            s.setCurseForgeStatus(getString(R.string.caramel_curseforge_searching));
        });
        CurseForgeBrowser.search(this, query, classId, new CurseForgeBrowser.SearchListener() {
            @Override
            public void onResults(List<CaramelCurseForgeItem> items) {
                mutateState(s -> {
                    s.setCurseForgeResults(items);
                    s.setCurseForgeBusy(false);
                    s.setCurseForgeSearched(true);
                    s.setCurseForgeStatus(items.isEmpty()
                            ? "" : getString(R.string.caramel_curseforge_result_count, items.size()));
                });
            }

            @Override
            public void onError(String message) {
                mutateState(s -> {
                    s.setCurseForgeBusy(false);
                    s.setCurseForgeSearched(true);
                    s.setCurseForgeStatus(getString(R.string.caramel_curseforge_failed, message));
                });
            }
        });
    }

    @Override
    public void downloadCurseForge(CaramelCurseForgeItem item) {
        if (item == null) return;
        if (!item.getDownloadable()) {
            toastWarning(getString(R.string.caramel_curseforge_no_file));
            return;
        }
        mutateState(s -> {
            s.setCurseForgeBusy(true);
            s.setCurseForgeDownloadingId(item.getId());
        });
        CurseForgeBrowser.download(this, item, new CurseForgeBrowser.DownloadListener() {
            @Override
            public void onFile(File file) {
                importDownloadedContent(file, item);
            }

            @Override
            public void onError(String message) {
                mutateState(s -> {
                    s.setCurseForgeBusy(false);
                    s.setCurseForgeDownloadingId(0);
                });
                toastWarning(getString(R.string.caramel_curseforge_failed, message));
            }
        });
    }

    /** Imports a downloaded add-on/world/pack into the selected instance's content folders. */
    private void importDownloadedContent(File file, CaramelCurseForgeItem item) {
        File data = activeGameDataDir();
        if (data == null) {
            mutateState(s -> {
                s.setCurseForgeBusy(false);
                s.setCurseForgeDownloadingId(0);
            });
            toastWarning(getString(R.string.caramel_curseforge_failed,
                    getString(R.string.caramel_no_instance)));
            return;
        }
        ContentImporter importer = new ContentImporter(this);
        importer.importContent(
                java.util.Collections.singletonList(Uri.fromFile(file)),
                new File(data, "resource_packs"),
                new File(data, "behavior_packs"),
                new File(data, "skin_packs"),
                new File(data, "minecraftWorlds"),
                new ContentImporter.ImportCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            mutateState(s -> {
                                s.setCurseForgeBusy(false);
                                s.setCurseForgeDownloadingId(0);
                            });
                            toast(getString(R.string.caramel_curseforge_imported, item.getName()));
                            refreshContentDirectories();
                            importer.shutdown();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            mutateState(s -> {
                                s.setCurseForgeBusy(false);
                                s.setCurseForgeDownloadingId(0);
                            });
                            toastWarning(getString(R.string.caramel_curseforge_failed, error));
                            importer.shutdown();
                        });
                    }

                    @Override
                    public void onProgress(int progress) {
                    }
                });
    }

    /**
     * Refreshes the on-screen Minecraft version label. Kept as a no-op so
     * {@code VersionManager.attemptRepairLibs} still compiles; the Caramel screen shows the version
     * on the hero, which {@link #refreshState()} drives.
     */
    public void setTextMinecraftVersion() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshContentDirectories();
        refreshSettingsPanel();
        refreshState();
    }

    @Override
    protected void onDestroy() {
        if (friendsClient != null) friendsClient.close();
        super.onDestroy();
    }
}
