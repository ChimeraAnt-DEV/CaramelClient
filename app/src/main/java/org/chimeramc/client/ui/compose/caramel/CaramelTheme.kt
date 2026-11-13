package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import org.chimeramc.client.R

/**
 * The Caramel design tokens: the navy-and-glass palette the UI pack defines, as Compose values.
 *
 * Ported from the UI pack's `dev.caramel.ui.theme`. The pack is a Compose *Desktop* project; the
 * values are the same, but the two font declarations are different here because Android resolves
 * fonts through `res/font` resources rather than a classpath path string.
 *
 * The palette lives in one object rather than in a Material `ColorScheme` because most of it —
 * the three-step text ramp, the hairline and fill alphas, the caramel accent — has no Material
 * name, and inventing `surfaceVariant`-style aliases for them would make the screens harder to
 * read, not easier.
 */
object CaramelColors {
    val Abyss = Color(0xFF060B1A)
    val Navy = Color(0xFF0A1330)
    val DeepBlue = Color(0xFF0F1B44)
    val Blue = Color(0xFF4A78FF)
    val BlueLight = Color(0xFF628CFF)
    val BlueDark = Color(0xFF3458E6)
    val Caramel = Color(0xFFF5A742)
    val CaramelDark = Color(0xFFDE8624)
    val Green = Color(0xFF2FD36B)
    val GreenLight = Color(0xFF3EE27A)
    val GreenDark = Color(0xFF18A050)
    val Red = Color(0xFFFF5468)
    val Orange = Color(0xFFFF9F2E)
    val Yellow = Color(0xFFFFD040)

    /** Primary text. */
    val Text = Color(0xFFEAF0FF)

    /** Secondary text: labels under a value, descriptions. */
    val Text2 = Color(0xFF9AA8D0)

    /** Tertiary text: captions, disabled. */
    val Text3 = Color(0xFF66749E)

    /** Text on a green fill, dark enough to pass contrast on [Green]. */
    val OnGreen = Color(0xFF062412)

    /** A 1dp border. */
    val Hairline = Color.White.copy(alpha = 0.14f)

    /** A quiet fill: an inactive control's track. */
    val Fill = Color.White.copy(alpha = 0.07f)

    /** A stronger fill: a hovered or pressed control. */
    val FillStrong = Color.White.copy(alpha = 0.12f)
}

/**
 * Poppins for display, Inter for controls and body.
 *
 * Android cannot load a font from an arbitrary path the way Desktop can, so these are the two
 * bundled `res/font` families. Both are the same typefaces the pack ships; the weight mapping is
 * done by the system from the single variable font rather than from four static files, which is why
 * each family is one resource here.
 */
object CaramelFonts {
    val Poppins = FontFamily(Font(R.font.poppins))
    val Inter = FontFamily(Font(R.font.inter))
}

/** The type ramp. Every style names its colour, so a caller does not repeat it at the call site. */
object CaramelType {
    val Display = TextStyle(fontFamily = CaramelFonts.Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CaramelColors.Text)
    val Title = TextStyle(fontFamily = CaramelFonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = CaramelColors.Text)
    val Subtitle = TextStyle(fontFamily = CaramelFonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = CaramelColors.Text)
    val Button = TextStyle(fontFamily = CaramelFonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = CaramelColors.Text)
    val Label = TextStyle(fontFamily = CaramelFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CaramelColors.Text)
    val Body = TextStyle(fontFamily = CaramelFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = CaramelColors.Text2)
    val Caption = TextStyle(fontFamily = CaramelFonts.Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = CaramelColors.Text3)
    val Chip = TextStyle(fontFamily = CaramelFonts.Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, color = CaramelColors.Text)
}

/**
 * The two blur sources a glass surface can sample.
 *
 * The pack splits them so a modal can blur the whole window while a card blurs only the wallpaper.
 * On Android there is no wallpaper to sample — the launcher owns its own backdrop — so both are
 * declared here for parity with the pack and a screen provides whichever it means. A screen that
 * never provides one and never renders a glass surface is fine: the locals are only read inside
 * [CaramelGlassPanel], so the error message names the real mistake.
 */
val LocalHaze = staticCompositionLocalOf<HazeState> {
    error("No HazeState provided: a glass surface needs one, see CaramelBackdrop")
}

val LocalWindowHaze = staticCompositionLocalOf<HazeState> {
    error("No window HazeState provided: a modal's glass surface needs one")
}
