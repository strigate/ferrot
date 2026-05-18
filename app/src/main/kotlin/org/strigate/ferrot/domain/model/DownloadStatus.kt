package org.strigate.ferrot.domain.model

enum class DownloadStatus {
    QUEUED,
    WAITING_FOR_NETWORK,
    WAITING_FOR_WIFI,
    METADATA,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    STOPPED,
}
