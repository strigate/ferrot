package org.strigate.ferrot.presentation.model

data class DownloadItemUiData(
    val id: Long,
    val title: String,
    val url: String,
    val status: DownloadStatusUiData,
    val progressFraction: Float?,
    val etaSeconds: Long?,
    val bytesDownloaded: Long,
    val expectedBytes: Long?,
)
