package org.strigate.ferrot.presentation.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class Dimens(
    val zero: Dp = 0.dp,

    val spacingXXSmall: Dp = 2.dp,
    val spacingXSmall: Dp = 4.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingMediumAlt: Dp = 12.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,

    val iconSmall: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconXLarge: Dp = 72.dp,
    val iconXXLarge: Dp = 96.dp,
    val iconXXXLarge: Dp = 128.dp,

    val radiusSmall: Dp = 4.dp,
    val radiusMedium: Dp = 8.dp,
    val radiusLarge: Dp = 16.dp,

    val tonalElevationLow: Dp = 1.dp,
    val tonalElevation: Dp = 2.dp,
    val tonalElevationHigh: Dp = 4.dp,

    val shadowElevationLow: Dp = 1.dp,
    val shadowElevation: Dp = 2.dp,
    val shadowElevationHigh: Dp = 4.dp,

    val contentMaxWidth: Dp = 320.dp,

    val overlayButtonSmall: Dp = 32.dp,
    val overlayButton: Dp = 56.dp,
    val overlayIcon: Dp = 48.dp,

    val thumbnailHeight: Dp = 240.dp,
    val actionIconSize: Dp = 38.dp,
    val dotSize: Dp = 8.dp,
)

val LocalDimens = staticCompositionLocalOf<Dimens> {
    error("Dimens not provided")
}
