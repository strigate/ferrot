package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadAudioEntity
import org.strigate.ferrot.domain.model.DownloadAudio

internal fun DownloadAudio.toEntity() = DownloadAudioEntity(
    id = 0L,
    downloadId = downloadId,
    filePath = filePath,
    fileExtension = fileExtension,
)

internal fun DownloadAudioEntity.toDomain() = DownloadAudio(
    downloadId = downloadId,
    filePath = filePath,
    fileExtension = fileExtension,
)
