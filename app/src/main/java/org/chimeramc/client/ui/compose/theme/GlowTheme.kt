package org.chimeramc.client.ui.compose.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.chimeramc.client.util.PersonalizationManager
import org.chimeramc.client.ui.components.UiTokens

/**
 * The Caramel Compose theme: navy surfaces, snow-blue accents.
 *
 * <p>Every value comes from [GlowPalette] and [UiTokens] so the Compose screens and the remaining
 * View screens read one palette. The theme reads the user's personalization (accent, AMOLED,
 * reduced motion, font scale) once per composition, so a settings change re-themes live.
 *
 * <p>The palette is exposed as [GlowThemeColors] through a static composition local, so a screen can
 * read the navy surface ramp and the snow-blue toggle colours that Material's scheme does not name.
 */
private val LocalGlowColors = staticCompositionLocalOf<GlowThemeColors> {
    error("GlowThemeColors requested outside GlowTheme")
}

private val LocalGlowMotion = staticCompositionLocalOf {
    GlowMotion(
        UiTokens.MOTION_FAST_MS.toInt(),
        UiTokens.MOTION_STANDARD_MS.toInt(),
        UiTokens.MOTION_SLOW_MS.toInt(),
        false,
    )
}

/** The extra tokens Material's [androidx.compose.material3.ColorScheme] does not carry. */
class GlowThemeColors(
    val window: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val stroke: Color,
    val snowBlue: Color,
    val snowBlueLight: Color,
    val snowBlueDeep: Color,
    val snowBlueWash: Color,
    val toggleTrackOff: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textOnSnow: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
) {
    /** The toggle track colour for a state, following the accent. */
    fun toggleTrack(on: Boolean, accent: Color): Color =
        if (on) Color(GlowPalette.toggleOnTrack(accent.toArgbInt())) else toggleTrackOff

    /** The toggle thumb colour for a state. */
    fun toggleThumb(on: Boolean): Color = Color(GlowPalette.toggleThumb(on))
}

/** The motion durations, from [UiTokens], so Compose and View screens animate alike. */
class GlowMotion(
    val fastMs: Int,
    val standardMs: Int,
    val slowMs: Int,
    val reduced: Boolean,
)

private fun Color.toArgbInt(): Int = (this.value shr 32).toInt()

/** The theme, applied around every Compose screen. */
@Composable
fun GlowTheme(
    context: Context = LocalContext.current,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val prefs = rememberPersonalization(context)
    val dark = isSystemInDarkTheme() || !prefs.light
    val scheme = GlowPalette.scheme(!dark, amoled || prefs.amoled)
    val accent = Color(GlowPalette.accent(prefs.accent))

    val glow = GlowThemeColors(
        window = Color(scheme.window),
        surface = Color(scheme.surface),
        surfaceRaised = Color(scheme.surfaceRaised),
        stroke = Color(scheme.stroke),
        snowBlue = Color(GlowPalette.SNOW_BLUE),
        snowBlueLight = Color(GlowPalette.SNOW_BLUE_LIGHT),
        snowBlueDeep = Color(GlowPalette.SNOW_BLUE_DEEP),
        snowBlueWash = Color(GlowPalette.SNOW_BLUE_WASH),
        toggleTrackOff = Color(GlowPalette.TOGGLE_TRACK_OFF),
        textPrimary = Color(GlowPalette.TEXT_PRIMARY),
        textSecondary = Color(GlowPalette.TEXT_SECONDARY),
        textDisabled = Color(GlowPalette.TEXT_DISABLED),
        textOnSnow = Color(GlowPalette.TEXT_ON_SNOW),
        success = Color(GlowPalette.SUCCESS),
        warning = Color(GlowPalette.WARNING),
        danger = Color(GlowPalette.DANGER),
    )

    val motion = if (prefs.reduceMotion) GlowMotion(120, 120, 120, true)
    else GlowMotion(
        UiTokens.MOTION_FAST_MS.toInt(),
        UiTokens.MOTION_STANDARD_MS.toInt(),
        UiTokens.MOTION_SLOW_MS.toInt(),
        false,
    )

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = glow.textOnSnow,
            secondary = glow.snowBlue,
            background = glow.window,
            onBackground = glow.textPrimary,
            surface = glow.surface,
            onSurface = glow.textPrimary,
            surfaceVariant = glow.surfaceRaised,
            onSurfaceVariant = glow.textSecondary,
            outline = glow.stroke,
            error = glow.danger,
        )
    } else {
        lightColorScheme(
            primary = glow.snowBlueDeep,
            onPrimary = Color.White,
            secondary = glow.snowBlueDeep,
            background = glow.window,
            onBackground = Color(0xFF0E1420),
            surface = glow.surface,
            onSurface = Color(0xFF0E1420),
            surfaceVariant = glow.surfaceRaised,
            onSurfaceVariant = Color(0xFF4A5568),
            outline = glow.stroke,
            error = glow.danger,
        )
    }

    CompositionLocalProvider(
        LocalGlowColors provides glow,
        LocalGlowMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = glowTypography(prefs.fontScale),
            shapes = glowShapes(),
            content = content,
        )
    }
}

/** The Caramel colours, read from any composable inside [GlowTheme]. */
val glowColors: GlowThemeColors
    @Composable @ReadOnlyComposable get() = LocalGlowColors.current

/** The Caramel motion, read from any composable inside [GlowTheme]. */
val glowMotion: GlowMotion
    @Composable @ReadOnlyComposable get() = LocalGlowMotion.current

private fun glowShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(UiTokens.RADIUS_SMALL.dp),
    small = RoundedCornerShape(UiTokens.RADIUS_MEDIUM.dp),
    medium = RoundedCornerShape(UiTokens.RADIUS_LARGE.dp),
    large = RoundedCornerShape(UiTokens.RADIUS_XLARGE.dp),
    extraLarge = RoundedCornerShape(UiTokens.RADIUS_XLARGE.dp),
)

private fun glowTypography(fontScale: Float): androidx.compose.material3.Typography {
    fun sp(v: Float) = (v * fontScale).sp
    return androidx.compose.material3.Typography(
        displaySmall = TextStyle(fontSize = sp(UiTokens.TEXT_DISPLAY),
            fontWeight = FontWeight(UiTokens.DISPLAY_WEIGHT)),
        titleMedium = TextStyle(fontSize = sp(UiTokens.TEXT_TITLE),
            fontWeight = FontWeight(UiTokens.TITLE_WEIGHT)),
        bodyMedium = TextStyle(fontSize = sp(UiTokens.TEXT_BODY),
            fontWeight = FontWeight(UiTokens.BODY_WEIGHT)),
        labelMedium = TextStyle(fontSize = sp(UiTokens.TEXT_LABEL),
            fontWeight = FontWeight(UiTokens.ROW_TITLE_WEIGHT)),
        labelSmall = TextStyle(fontSize = sp(UiTokens.TEXT_CAPTION),
            fontWeight = FontWeight(UiTokens.CAPTION_WEIGHT)),
    )
}

/** The personalization values the theme needs, read once per composition. */
private class GlowPrefs(
    val accent: Int?,
    val amoled: Boolean,
    val light: Boolean,
    val reduceMotion: Boolean,
    val fontScale: Float,
)

@Composable
private fun rememberPersonalization(context: Context): GlowPrefs {
    // Read on every composition rather than cached: the values are cheap preference reads and a
    // settings change must re-theme the screen that is already open.
    return androidx.compose.runtime.remember(context) {
        try {
            val pm = PersonalizationManager(context)
            val accent = pm.accentColor
            GlowPrefs(
                accent = if (accent != 0) accent else null,
                amoled = false,
                light = false,
                reduceMotion = pm.isReduceMotion,
                fontScale = pm.fontScale.coerceIn(0.85f, 1.6f),
            )
        } catch (t: Throwable) {
            // A theme must never be the reason a screen fails to draw.
            GlowPrefs(null, false, false, false, 1f)
        }
    }
}
