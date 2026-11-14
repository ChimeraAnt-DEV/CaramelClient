package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val Text = Color(0xFFEAF0FF)
    val Text2 = Color(0xFF9AA8D0)
    val Text3 = Color(0xFF66749E)
    val OnGreen = Color(0xFF062412)
    val Hairline = Color.White.copy(alpha = 0.14f)
    val Fill = Color.White.copy(alpha = 0.07f)
    val FillStrong = Color.White.copy(alpha = 0.12f)
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
    val Display = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.Text)
    val Title = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = C.Text)
    val Subtitle = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.Text)
    val Button = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = C.Text)
    val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = C.Text)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = C.Text2)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = C.Text3)
    val Chip = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp)
}

typealias T = CaramelType

/**
 * Installs the Caramel palette into Material so `Text`, `Icon` and the Material defaults resolve to
 * Caramel colours.
 *
 * <p>Most Caramel surfaces read [CaramelColors] directly — they draw things Material's scheme has no
 * slot for. This wrapper exists so a Material component dropped into the tree (a
 * `CircularProgressIndicator`, a ripple) does not fall back to the default purple.
 */
@Composable
fun CaramelTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
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
        ),
        content = content,
    )
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
