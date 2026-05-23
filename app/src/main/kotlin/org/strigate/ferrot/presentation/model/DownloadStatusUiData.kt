package org.strigate.ferrot.presentation.model

enum class DownloadStatusUiData {
    QUEUED,
    WAITING_FOR_NETWORK,
    WAITING_FOR_WIFI,
    METADATA,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    STOPPED,
}

val DownloadStatusUiData.isActive: Boolean
    get() = when (this) {
        DownloadStatusUiData.QUEUED,
        DownloadStatusUiData.WAITING_FOR_NETWORK,
        DownloadStatusUiData.WAITING_FOR_WIFI,
        DownloadStatusUiData.METADATA,
        DownloadStatusUiData.DOWNLOADING -> true

        DownloadStatusUiData.COMPLETED,
        DownloadStatusUiData.FAILED,
        DownloadStatusUiData.STOPPED -> false
    }

val DownloadStatusUiData.isFailed: Boolean
    get() = this == DownloadStatusUiData.FAILED
