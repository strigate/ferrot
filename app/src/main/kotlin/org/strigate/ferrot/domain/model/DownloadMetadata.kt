package org.strigate.ferrot.domain.model

data class DownloadMetadata(
    val downloadId: Long,
    val videoId: String?,
    val source: String?,
    val title: String?,
    val thumbnailFilePath: String?,
    val durationSeconds: Int?,
)
