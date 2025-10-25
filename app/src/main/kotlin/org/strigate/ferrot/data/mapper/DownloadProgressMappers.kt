package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadProgressEntity
import org.strigate.ferrot.domain.model.DownloadProgress

internal fun DownloadProgressEntity.toDomain() = DownloadProgress(
    downloadId = downloadId,
    progressPercent = progressPercent,
    etaSeconds = etaSeconds,
    bytesDownloaded = bytesDownloaded,
    expectedBytes = expectedBytes,
    updatedAtMillis = updatedAtMillis,
)

internal fun DownloadProgress.toEntity() = DownloadProgressEntity(
    downloadId = downloadId,
    progressPercent = progressPercent,
    etaSeconds = etaSeconds,
    bytesDownloaded = bytesDownloaded,
    expectedBytes = expectedBytes,
    updatedAtMillis = updatedAtMillis,
)
