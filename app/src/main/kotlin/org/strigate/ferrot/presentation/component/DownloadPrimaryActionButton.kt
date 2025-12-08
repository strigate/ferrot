package org.strigate.ferrot.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
fun DownloadPrimaryActionButton(
    status: DownloadStatusUiData,
    modifier: Modifier = Modifier,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val dimens = LocalDimens.current
    val actionConfig = when (status) {
        DownloadStatusUiData.QUEUED,
        DownloadStatusUiData.WAITING_FOR_NETWORK,
        DownloadStatusUiData.WAITING_FOR_WIFI,
        DownloadStatusUiData.METADATA,
        DownloadStatusUiData.DOWNLOADING -> ActionConfig(
            icon = Icons.Filled.Stop,
            contentDescription = stringResource(R.string.content_description_stop_download),
            usePrimaryTint = true,
            onClick = onPauseResume,
        )

        DownloadStatusUiData.PAUSED,
        DownloadStatusUiData.STOPPED,
        DownloadStatusUiData.FAILED -> ActionConfig(
            icon = Icons.Filled.Refresh,
            contentDescription = stringResource(R.string.content_description_resume_download),
            usePrimaryTint = true,
            onClick = onPauseResume,
        )

        DownloadStatusUiData.COMPLETED -> ActionConfig(
            icon = Icons.Filled.DownloadDone,
            contentDescription = stringResource(R.string.content_description_open_download),
            usePrimaryTint = false,
            onClick = onOpen,
        )
    }
    Surface(
        modifier = modifier
            .wrapContentSize(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = dimens.tonalElevationHigh,
    ) {
        with(actionConfig) {
            Box(
                modifier = Modifier
                    .size(dimens.iconXLarge),
            ) {
                IconButton(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Center),
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
}

private data class ActionConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val usePrimaryTint: Boolean,
    val onClick: () -> Unit,
)
