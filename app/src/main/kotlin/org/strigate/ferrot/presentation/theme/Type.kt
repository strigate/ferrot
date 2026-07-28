package org.strigate.ferrot.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TextStyles {
    @Composable
    fun dialogTitle() = MaterialTheme.typography.titleLarge
        .copy(
            fontSize = 22.sp,
            lineHeight = 24.sp,
        )

    @Composable
    fun downloadsTitle() = MaterialTheme.typography
        .titleLarge
        .copy(
            fontWeight = FontWeight.Normal,
        )
}
