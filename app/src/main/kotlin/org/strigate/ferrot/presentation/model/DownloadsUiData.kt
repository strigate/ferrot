package org.strigate.ferrot.presentation.model

data class DownloadsUiData(
    val downloads: List<DownloadItemUiData>,
    val availableUpdate: AvailableUpdateUiData?,
    val pendingDeleteIds: Set<Long>,
)
