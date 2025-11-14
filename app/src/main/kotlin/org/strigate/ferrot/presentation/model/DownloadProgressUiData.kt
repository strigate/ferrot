package org.strigate.ferrot.presentation.model

data class DownloadProgressUiData(
    val progressFraction: Float,
    val bytesDownloaded: Long,
    val etaSeconds: Long?,
    val expectedBytes: Long?,
)
