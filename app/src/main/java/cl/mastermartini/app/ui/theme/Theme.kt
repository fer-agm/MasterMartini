package cl.mastermartini.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import cl.mastermartini.app.data.Prefs
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import cl.mastermartini.app.R

val Green = Color(0, 77, 66)
val Gold = Color(193, 160, 29)
val Cream = Color(255,255,255)

private val LightScheme = lightColorScheme(
    primary = Green, onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE9), onPrimaryContainer = Color(0xFF00201B),
    secondary = Gold, onSecondary = Color.Black,
    background = Cream, onBackground = Color(0xFF1C1B1F),
    surface = Color.White, onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFB3261E)
)

private val ContrastScheme = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEB3B), onPrimaryContainer = Color.Black,
    secondary = Color(0xFF0033CC), onSecondary = Color.White,
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE), onSurfaceVariant = Color.Black,
    outline = Color.Black, error = Color(0xFFB00020)
)
val Roboto = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

private val MartiniTypography = with(Typography()) {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Roboto),
        displayMedium = displayMedium.copy(fontFamily = Roboto),
        displaySmall = displaySmall.copy(fontFamily = Roboto),
        headlineLarge = headlineLarge.copy(fontFamily = Roboto),
        headlineMedium = headlineMedium.copy(fontFamily = Roboto),
        headlineSmall = headlineSmall.copy(fontFamily = Roboto),
        titleLarge = titleLarge.copy(fontFamily = Roboto),
        titleMedium = titleMedium.copy(fontFamily = Roboto),
        titleSmall = titleSmall.copy(fontFamily = Roboto),
        bodyLarge = bodyLarge.copy(fontFamily = Roboto),
        bodyMedium = bodyMedium.copy(fontFamily = Roboto),
        bodySmall = bodySmall.copy(fontFamily = Roboto),
        labelLarge = labelLarge.copy(fontFamily = Roboto),
        labelMedium = labelMedium.copy(fontFamily = Roboto),
        labelSmall = labelSmall.copy(fontFamily = Roboto)
    )
}
@Composable
fun MartiniTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * prefs.textScale)) {
        MaterialTheme(
            colorScheme = if (prefs.highContrast) ContrastScheme else LightScheme,
            typography = MartiniTypography,
            content = content
        )
    }
}
