package org.strigate.ferrot.domain.model

data class DownloadWithMetadata(
    val id: Long,
    val url: String,
    val title: String,
    val status: DownloadStatus,
    val progressPercent: Float,
    val etaSeconds: Long?,
    val bytesDownloaded: Long,
    val expectedBytes: Long?,
)
