package org.strigate.ferrot.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadProgressBar(
    progress: Float?,
    running: Boolean,
    modifier: Modifier = Modifier,
    forcePrimary: Boolean = false,
) {
    val barColor = when {
        forcePrimary -> MaterialTheme.colorScheme.primary
        running -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    if (running && progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp),
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
                .height(4.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
