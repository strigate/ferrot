package org.strigate.ferrot.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
fun DownloadProgressBar(
    modifier: Modifier = Modifier,
    running: Boolean,
    progress: Float?,
    forcePrimary: Boolean = false,
) {
    val dimens = LocalDimens.current
    val barColor = when {
        forcePrimary -> MaterialTheme.colorScheme.primary
        running -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    if (running && progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(dimens.spacingXSmall),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        val target = (progress ?: 0f).coerceIn(0f, 1f)
        val animated by animateFloatAsState(targetValue = target, label = "downloadProgress")
        LinearProgressIndicator(
            progress = {
                when {
                    progress == null -> 0f
                    running -> animated
                    else -> target
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(dimens.spacingXSmall),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
