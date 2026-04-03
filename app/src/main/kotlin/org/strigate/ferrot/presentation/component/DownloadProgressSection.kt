package org.strigate.ferrot.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.strigate.ferrot.R
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.theme.LocalDimens
import org.strigate.ferrot.presentation.util.UiFormatter

@Composable
fun DownloadProgressSection(
    status: DownloadStatusUiData,
    progressFraction: Float?,
    etaSeconds: Long?,
    bytesDownloaded: Long,
    completedAtMillis: Long?,
    modifier: Modifier = Modifier,
    forcePrimaryBar: Boolean = false,
    alwaysShowBar: Boolean = true,
) {
    val dimens = LocalDimens.current
    val running = when (status) {
        DownloadStatusUiData.QUEUED,
        DownloadStatusUiData.METADATA,
        DownloadStatusUiData.DOWNLOADING -> true

        else -> false
    }
    val progress = when (status) {
        DownloadStatusUiData.DOWNLOADING -> progressFraction
        DownloadStatusUiData.COMPLETED -> 1f
        DownloadStatusUiData.STOPPED -> 0f
        else -> null
    }
    val showBar = running || alwaysShowBar
    Column(
        modifier = modifier,
    ) {
        if (showBar) {
            DownloadProgressBar(
                progress = progress,
                running = running,
                forcePrimary = forcePrimaryBar,
            )
            Spacer(modifier = Modifier.height(dimens.spacingXSmall))
        }
        Box(
            modifier = Modifier
                .heightIn(min = dimens.spacingLarge),
        ) {
            StatusSizeEtaRow(
                status = status,
                progressFraction = when (status) {
                    DownloadStatusUiData.COMPLETED -> 1f
                    DownloadStatusUiData.STOPPED -> 0f
                    else -> progressFraction
                },
                bytesDownloaded = bytesDownloaded,
                etaSeconds = etaSeconds,
                completedAtMillis = completedAtMillis,
            )
        }
    }
}

@Composable
private fun StatusSizeEtaRow(
    status: DownloadStatusUiData,
    progressFraction: Float?,
    bytesDownloaded: Long,
    etaSeconds: Long?,
    completedAtMillis: Long?,
) {
    val context = LocalContext.current
    val statusText = when (status) {
        DownloadStatusUiData.QUEUED -> stringResource(R.string.status_queued)
        DownloadStatusUiData.WAITING_FOR_NETWORK -> stringResource(R.string.status_waiting_for_network)
        DownloadStatusUiData.WAITING_FOR_WIFI -> stringResource(R.string.status_waiting_for_wifi)
        DownloadStatusUiData.METADATA -> stringResource(R.string.status_metadata)
        DownloadStatusUiData.DOWNLOADING -> stringResource(R.string.status_downloading)
        DownloadStatusUiData.PAUSED -> stringResource(R.string.status_paused)
        DownloadStatusUiData.COMPLETED -> stringResource(R.string.status_completed)
        DownloadStatusUiData.FAILED -> stringResource(R.string.status_failed)
        DownloadStatusUiData.STOPPED -> stringResource(R.string.status_stopped)
    }
    val sizeText = UiFormatter.formatBytes(bytesDownloaded)
    val etaText = UiFormatter.formatEta(etaSeconds)
    val leftText = buildList {
        add(statusText)
        add(sizeText)
        if (!etaText.isNullOrBlank() && status != DownloadStatusUiData.COMPLETED) {
            add(etaText)
        }
    }.joinToString(" · ")

    val rightText = when (status) {
        DownloadStatusUiData.COMPLETED -> completedAtMillis?.let {
            UiFormatter.formatCompletedAtTime(context, it)
        }

        DownloadStatusUiData.STOPPED -> stringResource(R.string.percent_0)
        else -> progressFraction?.let { "${(it * 100f).toInt().coerceIn(0, 100)}%" }
    }
    InfoLine(
        leftText = leftText,
        rightText = rightText,
        isError = status == DownloadStatusUiData.FAILED,
    )
}

@Composable
private fun InfoLine(
    leftText: String?,
    rightText: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = modifier
            .padding(top = dimens.spacingSmall)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!leftText.isNullOrBlank()) {
            Text(
                modifier = Modifier
                    .weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                text = leftText,
            )
        }
        if (!rightText.isNullOrBlank()) {
            Text(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                text = rightText,
            )
        }
    }
}
