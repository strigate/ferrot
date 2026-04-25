package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.view.DownloadWithMetadataView
import org.strigate.ferrot.domain.model.DownloadWithMetadata

internal fun DownloadWithMetadataView.toDomain() = DownloadWithMetadata(
    id = id,
    url = url,
    title = resolvedTitle,
    thumbnailFilePath = thumbnailFilePath,
    status = status.toDomain(),
    seen = seen,
    pendingDelete = pendingDelete,
    archived = archived,
    progressPercent = progressPercent,
    etaSeconds = etaSeconds,
    bytesDownloaded = bytesDownloaded,
    expectedBytes = expectedBytes,
    completedAtMillis = completedAtMillis,
)
