package org.strigate.ferrot.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val FerrotCoral = Color(0xFFFF8557)

private val LightColorScheme = lightColorScheme(
    primary = FerrotCoral,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFD5C8),
    onPrimaryContainer = Color(0xFF2F0A00),

    secondary = Color(0xFFFFB59F),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE3DA),
    onSecondaryContainer = Color(0xFF2F0E05),

    tertiary = Color(0xFFFFCABB),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE9E1),
    onTertiaryContainer = Color(0xFF321106),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1B1B1F),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF6ECE9),
    onSurfaceVariant = Color(0xFF51423D),
    surfaceContainer = Color(0xFFFAF6F4),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColorScheme = darkColorScheme(
    primary = FerrotCoral,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF7A3C2E),
    onPrimaryContainer = Color(0xFFFFDBCF),

    secondary = Color(0xFFFF9E80),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF6A3528),
    onSecondaryContainer = Color(0xFFFFD8CD),

    tertiary = Color(0xFFFFC2AE),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF6E3A2D),
    onTertiaryContainer = Color(0xFFFFE6DE),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE4E2E1),

    surface = Color(0xFF121212),
    onSurface = Color(0xFFE4E2E1),
    surfaceVariant = Color(0xFF2A2220),
    onSurfaceVariant = Color(0xFFD5C5BF),
    surfaceContainer = Color(0xFF1A1615),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun FerrotTheme(
//    dimens: Dimens = Dimens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
//        LocalDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) {
                DarkColorScheme
            } else {
                LightColorScheme
            },
            typography = Typography,
            content = content,
        )
    }
}
