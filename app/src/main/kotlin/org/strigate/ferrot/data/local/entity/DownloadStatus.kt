package org.strigate.ferrot.data.local.entity

enum class DownloadStatus {
    QUEUED,
    WAITING_FOR_NETWORK,
    WAITING_FOR_WIFI,
    METADATA,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    STOPPED,
}
