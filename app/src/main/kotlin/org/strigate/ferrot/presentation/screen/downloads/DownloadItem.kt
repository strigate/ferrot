package org.strigate.ferrot.presentation.screen.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import org.strigate.ferrot.presentation.component.DownloadPrimaryActionButton
import org.strigate.ferrot.presentation.component.DownloadProgressSection
import org.strigate.ferrot.presentation.model.DownloadItemUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.theme.LocalDimens

@Composable
internal fun DownloadListItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
    interactionEnabled: Boolean,
    longClickEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val dimens = LocalDimens.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = itemContainerColor(
            isSelected = isSelected,
            unselectedColor = MaterialTheme.colorScheme.background,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .downloadItemClick(
                    interactionEnabled = interactionEnabled,
                    longClickEnabled = longClickEnabled,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(
                    vertical = dimens.spacingSmall,
                    horizontal = dimens.spacingMedium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DownloadPrimaryActionButton(
                modifier = Modifier.size(dimens.downloadListThumbnailSize),
                thumbnailFilePath = item.thumbnailFilePath,
                status = item.status,
                enabled = interactionEnabled,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            Spacer(modifier = Modifier.width(dimens.spacingMediumAlt))
            val progressSpacing = if (item.status in inlineProgressStatuses) {
                dimens.spacingMediumAlt
            } else {
                dimens.spacingXXSmall
            }
            DownloadItemDetails(
                item = item,
                modifier = Modifier.weight(1f),
                progressSpacing = progressSpacing,
            )
        }
    }
}

@Composable
internal fun DownloadGridItem(
    item: DownloadItemUiData,
    isSelected: Boolean,
    interactionEnabled: Boolean,
    longClickEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
) {
    val dimens = LocalDimens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = itemContainerColor(
            isSelected = isSelected,
            unselectedColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        tonalElevation = dimens.tonalElevationLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .downloadItemClick(
                    interactionEnabled = interactionEnabled,
                    longClickEnabled = longClickEnabled,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            DownloadPrimaryActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
                thumbnailFilePath = item.thumbnailFilePath,
                status = item.status,
                enabled = interactionEnabled,
                onPauseResume = onPauseResume,
                onOpen = onOpen,
            )
            DownloadItemDetails(
                item = item,
                modifier = Modifier.padding(dimens.spacingSmall),
                progressSpacing = dimens.spacingSmall,
            )
        }
    }
}

@Composable
private fun DownloadItemDetails(
    item: DownloadItemUiData,
    progressSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DownloadItemTitle(item = item)
        Spacer(modifier = Modifier.height(progressSpacing))
        DownloadProgressSection(
            status = item.status,
            progressFraction = item.progressFraction,
            etaSeconds = item.etaSeconds,
            bytesDownloaded = item.bytesDownloaded,
            completedAtMillis = item.completedAtMillis,
            alwaysShowBar = false,
        )
    }
}

@Composable
private fun DownloadItemTitle(
    item: DownloadItemUiData,
) {
    val dimens = LocalDimens.current
    val showUnseen = !item.seen && item.status == DownloadStatusUiData.COMPLETED
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (showUnseen) FontWeight.Bold else FontWeight.Normal,
            ),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            text = item.title,
        )
        if (showUnseen) {
            Spacer(modifier = Modifier.width(dimens.spacingSmall))
            Box(
                modifier = Modifier
                    .size(dimens.dotSize)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

private fun Modifier.downloadItemClick(
    interactionEnabled: Boolean,
    longClickEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    return combinedClickable(
        enabled = interactionEnabled,
        onClick = onClick,
        onLongClick = onLongClick.takeIf { longClickEnabled },
    )
}

@Composable
private fun itemContainerColor(
    isSelected: Boolean,
    unselectedColor: Color,
): Color {
    return if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        unselectedColor
    }
}

private val inlineProgressStatuses = setOf(
    DownloadStatusUiData.QUEUED,
    DownloadStatusUiData.METADATA,
    DownloadStatusUiData.DOWNLOADING,
)
