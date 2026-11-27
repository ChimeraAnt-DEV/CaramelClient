package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.chimeramc.client.R

/**
 * The Caramel Client design tokens, ported from the `CaramelClient-UI-pack` Compose launcher.
 *
 * <p>The pack's `theme/Theme.kt` defines its palette as a flat `object C`. That is kept here rather
 * than folded into Material's `ColorScheme`, because most of what this UI draws (hairlines, fills,
 * glass sheens, the caramel accent) has no Material slot. The Material theme is still installed so
 * `Text`/`Icon` defaults resolve; the tokens below are what the Caramel surfaces actually read.
 *
 * <p>Colour values are byte-for-byte the pack's. Changing one here without changing the pack's
 * `Theme.kt` and `CaramelTheme.cpp` (the ImGui twin) is how the launcher and the in-game menu drift
 * apart, so the three are meant to move together.
 */
object CaramelColors {
    /**
     * Whether the light palette is active.
     *
     * <p>[CaramelTheme] resolves the user's night-mode choice once per composition and stores it
     * here, and every token below reads it. The launcher is recreated when the mode changes
     * (`ThemeManager.setThemeMode` calls `activity.recreate()`), so the whole tree is rebuilt with
     * the new palette; nothing has to observe this value.
     */
    @Volatile
    var light: Boolean = false

    /**
     * A white sheen in dark mode, a dark one in light mode.
     *
     * <p>The glass recipe is built from translucent white layers. On a light page white layers are
     * invisible, so the same surface would read as a flat rectangle; inverting the sheen keeps the
     * "light falls on glass" cue in both palettes without a second set of constants.
     */
    fun sheen(alpha: Float): Color =
        if (light) Color(0xFF0C1430).copy(alpha = alpha) else Color.White.copy(alpha = alpha)

    val Abyss: Color get() = if (light) Color(0xFFF2F5FB) else Color(0xFF060B1A)
    val Navy: Color get() = if (light) Color(0xFFFFFFFF) else Color(0xFF0A1330)
    val DeepBlue: Color get() = if (light) Color(0xFFE6ECF8) else Color(0xFF0F1B44)
    val Blue: Color get() = if (light) Color(0xFF3559E0) else Color(0xFF4A78FF)
    val BlueLight: Color get() = if (light) Color(0xFF4A6BE8) else Color(0xFF628CFF)
    val BlueDark: Color get() = if (light) Color(0xFF2743B8) else Color(0xFF3458E6)
    val Caramel: Color get() = if (light) Color(0xFFB96A10) else Color(0xFFF5A742)
    val CaramelDark: Color get() = if (light) Color(0xFF9A560B) else Color(0xFFDE8624)
    val Green: Color get() = if (light) Color(0xFF148A46) else Color(0xFF2FD36B)
    val GreenLight: Color get() = if (light) Color(0xFF22A85A) else Color(0xFF3EE27A)
    val GreenDark: Color get() = if (light) Color(0xFF0E6E38) else Color(0xFF18A050)
    val Red: Color get() = if (light) Color(0xFFC62B3E) else Color(0xFFFF5468)
    val Orange: Color get() = if (light) Color(0xFFB86A00) else Color(0xFFFF9F2E)
    val Yellow: Color get() = if (light) Color(0xFFC08A00) else Color(0xFFFFD040)
    val Text: Color get() = if (light) Color(0xFF0C1430) else Color(0xFFEAF0FF)
    val Text2: Color get() = if (light) Color(0xFF48567A) else Color(0xFF9AA8D0)
    val Text3: Color get() = if (light) Color(0xFF7784A4) else Color(0xFF66749E)
    val OnGreen: Color get() = if (light) Color.White else Color(0xFF062412)
    val Hairline: Color get() = sheen(0.14f)
    val Fill: Color get() = sheen(0.07f)
    val FillStrong: Color get() = sheen(0.12f)
}

/** Short alias, matching the pack's `C`. */
typealias C = CaramelColors

/**
 * The pack's type scale: Poppins for display and titles, Inter for controls and body.
 *
 * <p>Both families ship as variable fonts in `res/font`, so one file per family carries every
 * weight and the weight is selected by axis rather than by a second file.
 */
private val Poppins = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.Bold),
)

private val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

/** Short alias, matching the pack's `T`. */
object CaramelType {
    val Display: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = displayFamily(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.Text)
    val Title: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = displayFamily(), fontWeight = FontWeight.Medium, fontSize = 16.sp, color = C.Text)
    val Subtitle: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = displayFamily(), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.Text)
    val Button: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = displayFamily(), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.Text)
    val Label: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = bodyFamily(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = C.Text)
    val Body: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 12.sp, color = C.Text2)
    val Caption: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = bodyFamily(), fontWeight = FontWeight.Normal, fontSize = 11.sp, color = C.Text3)
    val Chip: TextStyle
        @Composable @ReadOnlyComposable get() = TextStyle(
            fontFamily = bodyFamily(), fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp)
}

typealias T = CaramelType

/**
 * The font the user picked, or null for the built-in families.
 *
 * <p>The "Font family" setting persists a [org.chimeramc.client.util.LauncherFonts] key, but the
 * Compose launcher reads its type from [CaramelType], which used to hard-code Poppins and Inter — so
 * the picker wrote a value nothing on the Compose surface ever read, which is why "changing the font
 * did nothing". [CaramelTheme] resolves the key to a bundled resource and provides it here; each role
 * then wears it, falling back to the built-in family when no face is chosen.
 */
private val LocalCaramelFamily = staticCompositionLocalOf<FontFamily?> { null }

/** The user's chosen face, or null when the built-in families are in use. */
val caramelFamily: FontFamily?
    @Composable @ReadOnlyComposable get() = LocalCaramelFamily.current

@Composable @ReadOnlyComposable
private fun displayFamily(): FontFamily = caramelFamily ?: Poppins

@Composable @ReadOnlyComposable
private fun bodyFamily(): FontFamily = caramelFamily ?: Inter

/** One family carrying the four weights the roles use, all from a single bundled variable font. */
private fun familyFor(fontRes: Int): FontFamily = FontFamily(
    Font(fontRes, FontWeight.Normal),
    Font(fontRes, FontWeight.Medium),
    Font(fontRes, FontWeight.SemiBold),
    Font(fontRes, FontWeight.Bold),
)

/**
 * Installs the Caramel palette into Material so `Text`, `Icon` and the Material defaults resolve to
 * Caramel colours.
 *
 * <p>Most Caramel surfaces read [CaramelColors] directly — they draw things Material's scheme has no
 * slot for. This wrapper exists so a Material component dropped into the tree (a
 * `CircularProgressIndicator`, a ripple) does not fall back to the default purple.
 */
@Composable
fun CaramelTheme(prefs: CaramelUiPrefs = rememberCaramelUiPrefs(), content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val context = LocalContext.current
    // The user's chosen face, resolved from the catalogue once per key. Null keeps the built-in
    // Poppins/Inter pairing, so the default look is unchanged.
    val family = remember(context, prefs.fontFamilyKey) {
        val res = org.chimeramc.client.util.LauncherFonts.loadableFontRes(context, prefs.fontFamilyKey)
        if (res != 0) familyFor(res) else null
    }
    // The night-mode choice resolves to one of two palettes. The launcher is recreated when the
    // choice changes, so resolving it here (rather than observing it) is enough.
    val light = org.chimeramc.client.util.ThemeManager.isLightActive(context)
    CaramelColors.light = light
    CompositionLocalProvider(
        LocalCaramelPrefs provides prefs,
        LocalCaramelFamily provides family,
        LocalDensity provides Density(density.density, prefs.fontScale),
    ) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = if (light) {
                androidx.compose.material3.lightColorScheme(
                    primary = C.Blue,
                    onPrimary = Color.White,
                    primaryContainer = C.DeepBlue,
                    onPrimaryContainer = C.Text,
                    secondary = C.Caramel,
                    onSecondary = Color.White,
                    background = C.Abyss,
                    onBackground = C.Text,
                    surface = C.Navy,
                    onSurface = C.Text,
                    surfaceVariant = C.DeepBlue,
                    onSurfaceVariant = C.Text2,
                    outline = C.Hairline,
                    error = C.Red,
                    onError = Color.White,
                )
            } else {
                androidx.compose.material3.darkColorScheme(
                    primary = C.Blue,
                    onPrimary = Color.White,
                    primaryContainer = C.DeepBlue,
                    onPrimaryContainer = C.Text,
                    secondary = C.Caramel,
                    onSecondary = Color.White,
                    background = C.Abyss,
                    onBackground = C.Text,
                    surface = C.Navy,
                    onSurface = C.Text,
                    surfaceVariant = C.DeepBlue,
                    onSurfaceVariant = C.Text2,
                    outline = C.Hairline,
                    error = C.Red,
                    onError = Color.White,
                )
            },
            content = content,
        )
    }
}

/**
 * The personalization values a Caramel surface reads.
 *
 * <p>Three of these (the font scale, the card radius and the glass strength) have no Material slot
 * and are read by [CaramelTheme] itself: the scale becomes the composition's font scale, the radius
 * is [GlassPanel]'s default corner, and the strength scales its frost. A settings screen passes its
 * own instance so moving a slider previews immediately, and every other screen uses the store.
 */
class CaramelUiPrefs(
    val fontScale: Float,
    /** The card corner radius in dp, from the user's "Card Corner Rounding". */
    val cardRadiusDp: Float,
    /** 1 while animation is on, 0 when the user asked for reduced motion. */
    val motionScale: Float,
    /** 0.35 to 1.35, from the user's "Glass blur strength". */
    val glassStrength: Float,
    /**
     * The user's "Animation Speed", 0.5 (half speed) to 3.0 (three times). A transition's base
     * duration is divided by this, so a larger value is a shorter animation, matching the
     * View-side {@code DynamicAnim} multiplier rather than inverting its sense.
     */
    val animationSpeed: Float = 1f,
    /**
     * The user's "UI Transparency", 0.0 (fully see-through) to 1.0 (opaque), default 0.85. It
     * scales the glass fills' opacity, which is the only honest reading on Android: the surfaces
     * are already translucent, so this is how much of the backdrop shows through them.
     */
    val uiTransparency: Float = 0.85f,
    /**
     * The chosen font key from [org.chimeramc.client.util.LauncherFonts], or empty for the built-in
     * families. When set, [CaramelTheme] replaces the display/title and body families with the
     * bundled face, so a font picked in Settings reaches the Compose launcher instead of only the
     * View-based screens.
     */
    val fontFamilyKey: String = "",
)

private val LocalCaramelPrefs = staticCompositionLocalOf {
    CaramelUiPrefs(fontScale = 1f, cardRadiusDp = 20f, motionScale = 1f, glassStrength = 1f, animationSpeed = 1f, uiTransparency = 0.85f)
}

/** The user's personalization, as [CaramelTheme] provides it. */
val caramelPrefs: CaramelUiPrefs
    @Composable @ReadOnlyComposable get() = LocalCaramelPrefs.current

/**
 * A Compose-observable mirror of [org.chimeramc.client.util.PersonalizationManager]'s change counter.
 *
 * <p>The store is plain Java over `SharedPreferences`, so a composition that reads it cannot see a
 * write: reading a static field does not register a snapshot read, and the surfaces would keep the
 * values they sampled on their first paint. The store bumps this counter alongside its own, which is
 * what makes a personalization change — a slider in the settings panel — repaint the glass it just
 * restyled, instead of waiting for the surface to be recreated.
 */
object CaramelPersonalizationSignal {
    var generation by mutableStateOf(0)
        private set

    init {
        // Registered once, when the object is first read from a composition. The store is a
        // singleton too, so a single listener sees every write from anywhere in the process.
        org.chimeramc.client.util.PersonalizationManager.setChangeListener { generation++ }
    }
}

/** Reads the persisted personalization; a failure yields the defaults rather than no theme. */
@Composable
fun rememberCaramelUiPrefs(): CaramelUiPrefs {
    val context = LocalContext.current
    val generation = CaramelPersonalizationSignal.generation
    return androidx.compose.runtime.remember(context, generation) {
        try {
            val pm = org.chimeramc.client.util.PersonalizationManager(context)
            CaramelUiPrefs(
                fontScale = pm.fontScale.coerceIn(0.8f, 1.6f),
                cardRadiusDp = pm.cardRounding.toFloat(),
                motionScale = if (pm.isReduceMotion) 0f else 1f,
                glassStrength = 0.35f + pm.blurIntensity / 100f,
                animationSpeed = pm.animationSpeed.coerceIn(0.5f, 3f),
                uiTransparency = (pm.uiTransparency / 100f).coerceIn(0f, 1f),
                fontFamilyKey = pm.fontFamily,
            )
        } catch (t: Throwable) {
            CaramelUiPrefs(fontScale = 1f, cardRadiusDp = 20f, motionScale = 1f, glassStrength = 1f, animationSpeed = 1f, uiTransparency = 0.85f)
        }
    }
}

/** The pack's spacing and radius scale. */
object CaramelDimens {
    val RadiusCard = 20.dp
    val RadiusHero = 24.dp
    val RadiusPanel = 18.dp
    val RadiusControl = 11.dp
    val RadiusButton = 12.dp
    val PagePad = 14.dp
    val Gap = 16.dp
    val GapLarge = 20.dp
}
