package org.strigate.ferrot.presentation.model

data class DownloadsUiData(
    val downloads: List<DownloadItemUiData>,
    val availableUpdate: AvailableUpdateUiData?,
)

data class AvailableUpdateUiData(
    val tag: String,
    val localFilePath: String?,
)
