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
    val spacingXSmallAlt: Dp = 6.dp,
    val spacingSmall: Dp = 8.dp,
    val spacingSmallAlt: Dp = 10.dp,
    val spacingMediumAlt: Dp = 12.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val spacingXLarge: Dp = 32.dp,
    val spacingXXLarge: Dp = 48.dp,

    val iconXXSmall: Dp = 16.dp,
    val iconXSmall: Dp = 24.dp,
    val iconSmall: Dp = 32.dp,
    val iconMedium: Dp = 48.dp,
    val iconLarge: Dp = 72.dp,
    val iconXLarge: Dp = 96.dp,
    val iconXXLarge: Dp = 128.dp,

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

    val downloadListThumbnailSize: Dp = 68.dp,
    val downloadListOverlayButtonSize: Dp = 28.dp,

    val thumbnailHeight: Dp = 240.dp,
    val actionIconSize: Dp = 38.dp,
    val dotSize: Dp = 8.dp,
)

val LocalDimens = staticCompositionLocalOf<Dimens> {
    error("Dimens not provided")
}
