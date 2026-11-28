package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * The launcher's plain-data shapes, ported from the pack's `data/Models.kt`.
 *
 * <p>The pack ships its own `Versions`, `ExampleMods` and `ExampleServers` here. Those are
 * stand-ins for a demo and are not carried over: the honesty rule forbids drawing a number or a
 * name the client has not actually measured or fetched, so every list this file describes is
 * filled by the activity from a real manager. What stays is the shape and the enumerations, so the
 * screens are a pure function of a small immutable value and can be tested on the JVM.
 */

/** A version's release channel, which is what the green/orange chip on a version row shows. */
enum class CaramelChannel(val label: String, val color: Color) {
    Release("Release", CaramelColors.Green),
    Beta("Beta", CaramelColors.Orange),
}

/** One selectable game version. [beta] drives the channel chip. */
data class CaramelVersion(val id: String, val beta: Boolean) {
    val channel: CaramelChannel get() = if (beta) CaramelChannel.Beta else CaramelChannel.Release
}

/**
 * The preloaders a game can be started with.
 *
 * <p>Only [Bedrock] exists: it is the shipped SweetLoader path the launcher actually runs. The
 * others are named so the choice is visible and are marked unavailable, because a loader that has
 * not been built cannot be started — offering it as a working option would be a lie the launch
 * could not keep.
 *
 * <p>[Road] is a loader that is still in development. Unlike the other unavailable choices it is
 * clickable, because pressing it is how the user is told what is happening: the tile shakes, its
 * timer turns red and the launcher raises a notice naming the development state.
 */
enum class CaramelLoader(val title: String, val available: Boolean, val inDevelopment: Boolean = false) {
    Bedrock("SweetLoader", true),
    Rust("RustyLoader", false),
    Lightning("LightningLoader", false),
    Road("RoadLoader", false, inDevelopment = true),
}

/** The instance profile mode: a shared "Open" profile or an isolated one. */
enum class CaramelProfileMode(val title: String, val desc: String, val label: String, val locked: Boolean) {
    Open("Open Instance Profile", "Shares worlds and settings", "Unlocked", false),
    Isolated("Isolated Instance Profile", "Separate files and mods", "Locked", true),
}

/** The launcher tabs the pack's sidebar carries, plus the CurseForge browser. */
enum class CaramelTab { Home, Mods, Servers, Cosmetics, News, Store, CurseForge, Settings }

/**
 * A transient in-app notice — the launcher's toast.
 *
 * <p>[text] is already-resolved display text (the caller reads the string resource), and [warning]
 * picks the leading mark: a red "!" in a rounded square for something that did not work, a blue
 * check for something that did. The notice clears itself, so a caller only ever raises one.
 */
data class CaramelNotice @JvmOverloads constructor(
    val text: String,
    val warning: Boolean = false,
)

/** One mod row: the fields the Mods grid and the instance-settings list draw. */
data class CaramelModInfo(
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val enabled: Boolean,
    /** The runtime label from [org.chimeramc.client.core.mods.ModRowPresentation.kindOf]. */
    val kind: String = "unknown",
    /**
     * Whether the mod's runtime is actually wired into the client. A `.universe` / `.singularity`
     * package is listed and toggleable but cannot run until the SweetLoader host table ships, so the
     * row shows "preview only" rather than a green "Enabled" that lies.
     */
    val runnable: Boolean = true,
)

/** One saved server: what the Servers tab draws. [online] and [ping] are only set when measured. */
data class CaramelServerInfo(
    val name: String,
    val address: String,
    val port: Int,
    val online: Int?,
    val ping: Int?,
    /** The game version the server advertised, empty until a ping measured one. */
    val version: String,
    val favourite: Boolean,
    /** The icon key from [org.chimeramc.client.core.content.ServerIconCatalog], empty when none. */
    val iconKey: String,
    /** True for a shipped default server, false for one the user saved. */
    val featured: Boolean,
    /** True once a ping has been attempted, so the row can tell "checking" from "offline". */
    val checked: Boolean = false,
)

/** What kind of cosmetic a tile is, which is the Cosmetics screen's filter chip. */
enum class CaramelCosmeticKind(val chip: String) {
    Cape("Capes"),
    Wings("Wings"),
    Hat("Hats"),
    Pet("Pets"),
}

/** One cosmetic tile: the Cosmetics grid draws these from the real catalogue. */
data class CaramelCosmeticInfo(
    val id: String,
    val name: String,
    val kind: CaramelCosmeticKind,
    val equipped: Boolean,
    val owned: Boolean,
    val favourite: Boolean,
)

/** One news post: the News screen and the news modal both draw these. */
data class CaramelNewsInfo @JvmOverloads constructor(
    val title: String,
    val summary: String,
    val meta: String,
    val featured: Boolean,
    /**
     * An optional in-app action: the button's label, empty when the post has none.
     *
     * <p>A post with a label and a [actionUrl] draws a button that opens the page in the launcher's
     * own WebView, so a link never throws the user out to a browser.
     */
    val actionLabel: String = "",
    val actionUrl: String = "",
    /**
     * The post's banner drawable, or 0 when it has none.
     *
     * <p>A post with a banner paints it; a post without one keeps the generated gradient, so the
     * card never shows an empty hole where art is missing.
     */
    val imageRes: Int = 0,
)

/**
 * One CurseForge project, as the CurseForge tab draws it.
 *
 * <p>[fileId] and [downloadUrl] are the newest file the API returned. A project with no file is
 * still listed (it exists) but its Download button is disabled rather than pretending there is
 * something to fetch.
 */
data class CaramelCurseForgeItem @JvmOverloads constructor(
    val id: Int,
    val name: String,
    val summary: String,
    val author: String,
    val downloads: Long,
    val logoUrl: String = "",
    val fileId: Int = 0,
    val fileName: String = "",
    val downloadUrl: String = "",
    val websiteUrl: String = "",
) {
    val downloadable: Boolean get() = downloadUrl.isNotEmpty()
}

/** One captured screenshot: the Screenshots modal draws these from the instance's folder. */
data class CaramelScreenshotInfo(
    val name: String,
    val path: String,
)

/** One group of release notes: a heading and the lines under it. */
data class CaramelReleaseSection(
    val title: String,
    val lines: List<String>,
)

/**
 * The current release's notes, shown by the Release notes popup.
 *
 * <p>Filled from the bundled `launcher/changelog.json` (the same document the first-run card reads)
 * and the running build's own version, so the popup describes the build the user is on rather than a
 * hard-coded list. [isEmpty] is true when the document carried nothing to show.
 */
data class CaramelReleaseNotes(
    val version: String,
    val headline: String,
    val summary: String,
    val dateLine: String,
    val sections: List<CaramelReleaseSection>,
) {
    val isEmpty: Boolean
        get() = headline.isEmpty() && summary.isEmpty() && sections.isEmpty()
}

/** One Xbox Live friend: the Friends card and modal draw these from PeopleHub. */
/**
 * How the signed-in account and this one relate on Xbox Live.
 *
 * <p>Xbox has no request object: a follow that is not returned is a request. Without this the three
 * cases drew identically, so a request - either direction - looked like a friend, and the requests
 * screen had nothing to be wrong about.
 */
enum class CaramelFriendRelation { FRIEND, REQUEST_RECEIVED, REQUEST_SENT }

data class CaramelFriendInfo(
    val xuid: String,
    val gamertag: String,
    val avatarUrl: String,
    val online: Boolean,
    val presence: String,
    /**
     * True when this account has been heard on a Caramel-only protocol, so the row may wear the
     * Caramel badge. False for an ordinary Xbox account, which is the honest answer: the badge is a
     * claim that the account runs CaramelClient, and only a Caramel channel is evidence of that.
     */
    val caramel: Boolean = false,
    /** Whether this account is a mutual friend or a request in either direction. */
    val relation: CaramelFriendRelation = CaramelFriendRelation.FRIEND,
)

/**
 * The launcher's whole mutable state, as the pack's `LauncherState` is.
 *
 * <p>Every list defaults to empty and every count to zero: the screens then draw their empty state
 * rather than a fabricated row. The activity fills them from the live managers.
 */
class CaramelLauncherState {
    var tab by mutableStateOf(CaramelTab.Home)

    var versions by mutableStateOf<List<CaramelVersion>>(emptyList())
    var version by mutableStateOf<CaramelVersion?>(null)
    var loader by mutableStateOf(CaramelLoader.Bedrock)
    var profile by mutableStateOf(CaramelProfileMode.Isolated)

    var instanceName by mutableStateOf("")
    var accountName by mutableStateOf("")
    var signedIn by mutableStateOf(false)

    /** The signed-in account's profile picture URL (Xbox), empty when unknown or signed out. */
    var accountAvatarUrl by mutableStateOf("")
    /**
     * How many instances the launcher knows about — installed plus imported custom ones.
     *
     * <p>This is the number the title bar shows. It used to be a running-game flag, so it read
     * "0 instances" for every user who was not mid-game, which is everyone almost all of the time.
     */
    var instancesCount by mutableStateOf(0)
    var launchEnabled by mutableStateOf(false)
    var launching by mutableStateOf(false)

    var mods by mutableStateOf<List<CaramelModInfo>>(emptyList())
    var servers by mutableStateOf<List<CaramelServerInfo>>(emptyList())
    var cosmetics by mutableStateOf<List<CaramelCosmeticInfo>>(emptyList())
    var news by mutableStateOf<List<CaramelNewsInfo>>(emptyList())
    var screenshots by mutableStateOf<List<CaramelScreenshotInfo>>(emptyList())
    var friends by mutableStateOf<List<CaramelFriendInfo>>(emptyList())

    /** A status line for the friends list: "loading", an error reason, or empty when loaded. */
    var friendsStatus by mutableStateOf("")

    var launchMenu by mutableStateOf(false)
    var instanceSettings by mutableStateOf(false)
    var accountMenu by mutableStateOf(false)
    var addFriend by mutableStateOf(false)
    var launcherSettings by mutableStateOf(false)
    var appMenu by mutableStateOf(false)
    var screenshotsModal by mutableStateOf(false)
    var friendsAll by mutableStateOf(false)

    /** Whether the "View all news" modal is open. */
    var newsAll by mutableStateOf(false)

    /** The post the News screen shows in its featured slot; empty means "the newest featured". */
    var featuredNewsTitle by mutableStateOf("")

    /**
     * The current release's notes, shown by the Release notes popup.
     *
     * <p>Read once from the bundled changelog when the launcher mounts, so the menu item is a
     * property of the build rather than something re-read on every open.
     */
    var releaseNotes by mutableStateOf(CaramelReleaseNotes("", "", "", "", emptyList()))

    /** Whether the Release notes popup is open. */
    var releaseNotesOpen by mutableStateOf(false)

    /** Whether the About modal is open. */
    var aboutOpen by mutableStateOf(false)

    /**
     * The page the in-app browser is showing, empty when it is closed.
     *
     * <p>A news post's link opens here rather than in the system browser, so the user stays in the
     * launcher. The host fills [webTitle] with the page's own name.
     */
    var webUrl by mutableStateOf("")

    /** The title shown on the in-app browser's header, empty falls back to the URL. */
    var webTitle by mutableStateOf("")
    var addAccount by mutableStateOf(false)
    var accountSettings by mutableStateOf(false)

    /** The selected mod row, or null when the detail modal is closed. */
    var selectedMod by mutableStateOf<CaramelModInfo?>(null)

    /** The selected screenshot row, or null when the Screenshots modal is closed. */
    var selectedScreenshot by mutableStateOf<CaramelScreenshotInfo?>(null)

    /** Whether the instance's files can be repaired or deleted; false for the built-in version. */
    var instanceEditable by mutableStateOf(false)

    /** A status line shown while a long action (sign-in, repair) is running; empty when idle. */
    var busyMessage by mutableStateOf("")

    /** Import progress, 0-100, or -1 when no instance import is running. */
    var importProgress by mutableStateOf(-1)

    /** The Microsoft device code shown in the add-account modal, empty when no sign-in is running. */
    var deviceCode by mutableStateOf("")

    /** The page the code is entered on, with the code pre-filled; empty until a code is issued. */
    var deviceCodeUrl by mutableStateOf("")

    /** Whether a sign-in is in flight, so the modal can show progress rather than a dead button. */
    var signInRunning by mutableStateOf(false)

    /** Why the last sign-in attempt failed, empty when it has not failed. */
    var signInError by mutableStateOf("")

    /**
     * True while the sign-in modal is showing the in-app Microsoft page.
     *
     * <p>The device-code flow used to hand the user to the system browser the moment Microsoft
     * issued a code, so they left the launcher to finish signing in. The launcher already hosts its
     * own WebView for news links; the sign-in page opens in that same in-app surface instead, and
     * this flag is what makes the modal swap from the code panel to the page.
     */
    var signInWebOpen by mutableStateOf(false)

    /** The gamertag typed into the Add friend modal. */
    var friendQuery by mutableStateOf("")

    /** Whether a gamertag lookup is in flight, so the modal can show progress. */
    var friendSearchRunning by mutableStateOf(false)

    /** The account the last lookup resolved, or null when nothing has matched yet. */
    var friendFound by mutableStateOf<CaramelFoundUser?>(null)

    /** Whether the found account is a confirmed Caramel user, so the badge is honest. */
    var friendFoundIsCaramel by mutableStateOf(false)

    /** Why the last lookup found nothing, empty when it has not failed. */
    var friendSearchError by mutableStateOf("")

    /** Whether the follow request for [friendFound] is in flight. */
    var friendRequestRunning by mutableStateOf(false)

    /** The result line shown after a follow attempt, empty when idle. */
    var friendRequestResult by mutableStateOf("")

    /** Whether the follow request succeeded, so the result line can be tinted. */
    var friendRequestSucceeded by mutableStateOf(false)

    /** Whether the in-app mods-folder browser is open. */
    var modsFolderOpen by mutableStateOf(false)

    /** Whether the Add server modal is open. */
    var addServer by mutableStateOf(false)

    /**
     * The in-app notice currently on screen, or null when none is.
     *
     * <p>This is the launcher's toast: a transient line that appears over the frame and clears
     * itself. It is deliberately part of the state so a host action can raise one from anywhere.
     */
    var notice by mutableStateOf<CaramelNotice?>(null)

    /** The name typed into the Add server modal. */
    var serverDraftName by mutableStateOf("")

    /** The address typed into the Add server modal. */
    var serverDraftAddress by mutableStateOf("")

    /** The port typed into the Add server modal, as text so the field can be cleared. */
    var serverDraftPort by mutableStateOf("")

    /** Whether an add-server write is in flight, so the button shows progress. */
    var addServerRunning by mutableStateOf(false)

    /** Why the last add-server attempt failed, empty when it has not failed. */
    var addServerError by mutableStateOf("")

    /** The folder the browser is currently showing, as a path relative to the mods root. */
    var modsFolderPath by mutableStateOf("/")

    /** The entries in the folder the browser is showing. */
    var modsFolderEntries by mutableStateOf<List<CaramelFileEntry>>(emptyList())

    /** Whether the browser can go up a level (false at the mods root). */
    var modsFolderCanGoUp by mutableStateOf(false)

    /**
     * The instance's performance settings, keyed by [org.chimeramc.client.core.minecraft.PerfFeatureCatalog]
     * feature id. A missing entry means the instance's options file has no such key yet, so the tab
     * draws the feature's own default rather than a fabricated reading.
     */
    var perfValues by mutableStateOf<Map<String, String>>(emptyMap())

    /** A status line for the last performance write; empty when idle. */
    var perfStatus by mutableStateOf("")

    /** Whether this instance runs the MB shader loader (the mtbinloader2 compatibility fixer). */
    var instanceShaderCompat by mutableStateOf(false)

    /**
     * Whether this device can run the deferred (Vibrant Visuals) pipeline. When false, the
     * Performance tab does not offer the Vibrant Visuals option, so a mode the GPU cannot render is
     * never advertised as if it were available.
     */
    var deferredRenderSupported by mutableStateOf(false)

    /** Whether this instance keeps its own game data rather than the shared root. */
    var instanceIsolation by mutableStateOf(false)

    /** Whether this instance launches in portrait. */
    var instanceLaunchVertical by mutableStateOf(false)

    /** Whether this instance exposes the in-game resource-pack changer. */
    var instancePackChanger by mutableStateOf(false)

    // --- CurseForge browser ---------------------------------------------------------------

    /** The text in the CurseForge search box, as typed. */
    var curseForgeQuery by mutableStateOf("")

    /** The projects the last CurseForge search returned. */
    var curseForgeResults by mutableStateOf<List<CaramelCurseForgeItem>>(emptyList())

    /** A status line under the search box: "Searching", an error, or the result count. */
    var curseForgeStatus by mutableStateOf("")

    /** True while a search or a download is in flight, so the screen can show progress. */
    var curseForgeBusy by mutableStateOf(false)

    /** The project id being downloaded, or 0 when nothing is downloading. */
    var curseForgeDownloadingId by mutableStateOf(0)

    /** True once a search has run, so the empty state can tell "not searched" from "no matches". */
    var curseForgeSearched by mutableStateOf(false)

    /** The catalogue section the tab is browsing; [CurseForgeSection.ALL] until one is picked. */
    var curseForgeSection by mutableStateOf(CurseForgeSection.ALL)

    // --- Update check ---------------------------------------------------------------------
}

/** One row in the mods-folder browser: a file or a sub-directory. */
data class CaramelFileEntry(
    val name: String,
    val path: String,
    val directory: Boolean,
    val sizeLabel: String,
)

/** One account the Add friend modal resolved from a typed gamertag. */
data class CaramelFoundUser(
    val xuid: String,
    val gamertag: String,
    val avatarUrl: String,
)
