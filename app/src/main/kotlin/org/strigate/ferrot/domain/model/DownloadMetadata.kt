package org.strigate.ferrot.domain.model

data class DownloadMetadata(
    val downloadId: Long,
    val title: String?,
    val thumbnailFilePath: String?,
)
