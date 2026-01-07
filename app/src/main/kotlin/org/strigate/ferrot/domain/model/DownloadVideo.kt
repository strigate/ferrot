package org.strigate.ferrot.domain.model

data class DownloadVideo(
    val downloadId: Long,
    val filePath: String,
    val fileExtension: String,
    val sha256: String?,
)
