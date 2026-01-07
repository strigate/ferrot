package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadVideoEntity
import org.strigate.ferrot.domain.model.DownloadVideo

internal fun DownloadVideo.toEntity() = DownloadVideoEntity(
    id = 0L,
    downloadId = downloadId,
    filePath = filePath,
    fileExtension = fileExtension,
    sha256 = sha256,
)

internal fun DownloadVideoEntity.toDomain() = DownloadVideo(
    downloadId = downloadId,
    filePath = filePath,
    fileExtension = fileExtension,
    sha256 = sha256,
)
