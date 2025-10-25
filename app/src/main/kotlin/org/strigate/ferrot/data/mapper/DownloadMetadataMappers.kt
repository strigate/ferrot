package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadMetadataEntity
import org.strigate.ferrot.domain.model.DownloadMetadata

internal fun DownloadMetadataEntity.toDomain() = DownloadMetadata(
    downloadId = downloadId,
    title = title,
    thumbnailFilePath = thumbnailFilePath,
)

internal fun DownloadMetadata.toEntity() = DownloadMetadataEntity(
    downloadId = downloadId,
    title = title,
    thumbnailFilePath = thumbnailFilePath,
)
