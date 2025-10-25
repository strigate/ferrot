package org.strigate.ferrot.presentation.model

data class DownloadUiData(
    val title: String,
    val url: String,
    val filePath: String?,
    val fileName: String?,
    val status: DownloadStatusUiData,
    val errorMessage: String?,
    val progressFraction: Float?,
    val bytesDownloaded: Long,
    val etaSeconds: Long?,
    val expectedBytes: Long?,
    val thumbnailFilePath: String?,
)
