package org.strigate.ferrot.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

object TextStyles {
    @Composable
    fun downloadsTitle() = MaterialTheme.typography
        .titleLarge
        .copy(
            fontWeight = FontWeight.Normal,
        )
}
