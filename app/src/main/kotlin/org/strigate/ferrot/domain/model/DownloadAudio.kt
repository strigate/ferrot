package org.strigate.ferrot.domain.model

data class DownloadAudio(
    val downloadId: Long,
    val filePath: String,
    val fileExtension: String,
)
