package org.strigate.ferrot.presentation.model

data class DownloadsUiData(
    val downloads: List<DownloadItemUiData>,
    val availableUpdate: AvailableUpdateUiData?,
    val pendingDeleteIds: Set<Long>,
    val retryFailedDownloadIds: Set<Long>,
    val gridLayoutEnabled: Boolean,
    val leftSwipeAction: DownloadSwipeActionUiData,
    val rightSwipeAction: DownloadSwipeActionUiData,
)
