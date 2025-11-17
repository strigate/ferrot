package org.strigate.ferrot.presentation.model

data class DownloadUiData(
    val url: String,
    val status: DownloadStatusUiData,
    val metadata: DownloadMetadataUiData?,
    val video: DownloadVideoUiData?,
    val audio: DownloadAudioUiData?,
    val progress: DownloadProgressUiData?,
    val errorMessage: String?,
)
