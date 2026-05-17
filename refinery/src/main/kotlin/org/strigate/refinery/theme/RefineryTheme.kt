package org.strigate.refinery.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val RefineryPrimary = Color(0xFFFF8557)

private val LightColorScheme = lightColorScheme(
    primary = RefineryPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7DF),
    onPrimaryContainer = Color(0xFF2B140D),

    secondary = Color(0xFF5F5F66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E7EC),
    onSecondaryContainer = Color(0xFF1C1C21),

    tertiary = Color(0xFF74747D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAEAF0),
    onTertiaryContainer = Color(0xFF1D1D23),

    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF18181B),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFE1E3E8),
    onSurfaceVariant = Color(0xFF44444B),
    surfaceContainer = Color(0xFFF1F1F4),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF2F2B2A),
    inverseOnSurface = Color(0xFFF8EEEA),
    inversePrimary = RefineryPrimary,

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColorScheme = darkColorScheme(
    primary = RefineryPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C3125),
    onPrimaryContainer = Color(0xFFFFE7DF),

    secondary = Color(0xFFC7C6CC),
    onSecondary = Color(0xFF2E2E33),
    secondaryContainer = Color(0xFF3A3A40),
    onSecondaryContainer = Color(0xFFE4E4E9),

    tertiary = Color(0xFFCBCBD2),
    onTertiary = Color(0xFF303036),
    tertiaryContainer = Color(0xFF3D3D44),
    onTertiaryContainer = Color(0xFFE8E8EE),

    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDF0),

    surface = Color(0xFF101114),
    onSurface = Color(0xFFF1F1F4),
    surfaceVariant = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFFA8A8B0),
    surfaceContainer = Color(0xFF17171B),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF8EEEA),
    inverseOnSurface = Color(0xFF2A1F1B),
    inversePrimary = Color(0xFFFF6F3C),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun RefineryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dimens: RefineryDimens = RefineryDimens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRefineryDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            },
            typography = RefineryTypography,
            content = content,
        )
    }
}
