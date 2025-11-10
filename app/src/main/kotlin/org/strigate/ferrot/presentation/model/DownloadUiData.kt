package org.strigate.ferrot.presentation.model

data class DownloadUiData(
    val title: String,
    val url: String,
    val videoFilePath: String?,
    val videoFileName: String?,
    val audioFilePath: String?,
    val audioFileName: String?,
    val status: DownloadStatusUiData,
    val errorMessage: String?,
    val progressFraction: Float?,
    val bytesDownloaded: Long,
    val etaSeconds: Long?,
    val expectedBytes: Long?,
    val thumbnailFilePath: String?,
)
