package org.strigate.ferrot.presentation.model

data class DownloadItemUiData(
    val id: Long,
    val url: String,
    val title: String,
    val thumbnailFilePath: String?,
    val status: DownloadStatusUiData,
    val seen: Boolean,
    val progressFraction: Float?,
    val etaSeconds: Long?,
    val bytesDownloaded: Long,
    val expectedBytes: Long?,
    val completedAtMillis: Long?,
)
