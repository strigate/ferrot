package org.strigate.ferrot.presentation.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class Dimens(
    val zero: Dp = 0.dp,

    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,

    val iconSmall: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconXLarge: Dp = 96.dp,
    val iconXXLarge: Dp = 128.dp,

    val radiusSmall: Dp = 4.dp,
    val radiusMedium: Dp = 8.dp,
    val radiusLarge: Dp = 16.dp,

    val stroke: Dp = 1.dp,

    val contentMaxWidth: Dp = 320.dp,
)

val LocalDimens = staticCompositionLocalOf<Dimens> {
    error("Dimens not provided")
}
