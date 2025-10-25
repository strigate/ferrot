package org.strigate.ferrot.domain.model

data class DownloadProgress(
    val downloadId: Long,
    val updatedAtMillis: Long,
    val progressPercent: Float,
    val bytesDownloaded: Long,
    val etaSeconds: Long?,
    val expectedBytes: Long?,
)
