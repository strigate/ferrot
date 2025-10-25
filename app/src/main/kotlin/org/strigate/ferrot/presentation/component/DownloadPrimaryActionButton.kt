package org.strigate.ferrot.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.strigate.ferrot.presentation.model.DownloadStatusUiData

@Composable
fun DownloadPrimaryActionButton(
    status: DownloadStatusUiData,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionConfig: ActionConfig = when (status) {
        DownloadStatusUiData.QUEUED,
        DownloadStatusUiData.WAITING_FOR_NETWORK,
        DownloadStatusUiData.WAITING_FOR_WIFI,
        DownloadStatusUiData.METADATA,
        DownloadStatusUiData.DOWNLOADING -> ActionConfig(
            icon = Icons.Filled.Stop,
            contentDescription = "Stop download",
            usePrimaryTint = true,
            onClick = onPauseResume,
        )

        DownloadStatusUiData.PAUSED,
        DownloadStatusUiData.STOPPED,
        DownloadStatusUiData.FAILED -> ActionConfig(
            icon = Icons.Filled.Refresh,
            contentDescription = "Resume download",
            usePrimaryTint = true,
            onClick = onPauseResume,
        )

        DownloadStatusUiData.COMPLETED -> ActionConfig(
            icon = Icons.Filled.DownloadDone,
            contentDescription = "Open",
            usePrimaryTint = false,
            onClick = onOpen,
        )
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        with(actionConfig) {
            IconButton(
                onClick = onClick,
            ) {
                Icon(
                    imageVector = icon,
                    tint = if (usePrimaryTint) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

private data class ActionConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val usePrimaryTint: Boolean,
    val onClick: () -> Unit,
)
