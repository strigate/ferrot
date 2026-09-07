package org.strigate.ferrot.presentation.model

data class DownloadItemUiData(
    val id: Long,
    val title: String,
    val thumbnailFilePath: String?,
    val status: DownloadStatusUiData,
    val seen: Boolean,
    val progressFraction: Float?,
    val etaSeconds: Long?,
    val bytesDownloaded: Long,
    val completedAtMillis: Long?,
)
