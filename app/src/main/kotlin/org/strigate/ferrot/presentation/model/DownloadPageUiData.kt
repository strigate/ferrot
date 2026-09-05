package org.strigate.ferrot.presentation.model

data class DownloadPageUiData(
    val id: Long,
    val url: String,
    val status: DownloadStatusUiData,
    val metadata: DownloadMetadataUiData?,
    val video: DownloadVideoUiData?,
    val audio: DownloadAudioUiData?,
    val progress: DownloadProgressUiData?,
    val seen: Boolean,
    val archived: Boolean,
    val errorMessage: String?,
    val completedAtMillis: Long?,
    val thumbnailAvailable: Boolean = false,
)
