package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.presentation.model.DownloadStatusUiData

fun DownloadStatus.toUiData(): DownloadStatusUiData = when (this) {
    DownloadStatus.QUEUED -> DownloadStatusUiData.QUEUED
    DownloadStatus.WAITING_FOR_NETWORK -> DownloadStatusUiData.WAITING_FOR_NETWORK
    DownloadStatus.WAITING_FOR_WIFI -> DownloadStatusUiData.WAITING_FOR_WIFI
    DownloadStatus.METADATA -> DownloadStatusUiData.METADATA
    DownloadStatus.DOWNLOADING -> DownloadStatusUiData.DOWNLOADING
    DownloadStatus.PAUSED -> DownloadStatusUiData.PAUSED
    DownloadStatus.COMPLETED -> DownloadStatusUiData.COMPLETED
    DownloadStatus.FAILED -> DownloadStatusUiData.FAILED
    DownloadStatus.STOPPED -> DownloadStatusUiData.STOPPED
}
