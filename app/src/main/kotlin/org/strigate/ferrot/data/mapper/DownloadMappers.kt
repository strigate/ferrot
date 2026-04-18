package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadEntity
import org.strigate.ferrot.domain.model.Download

internal fun DownloadEntity.toDomain() = Download(
    id = id,
    uid = uid,
    url = url,
    status = status.toDomain(),
    seen = seen,
    pendingDelete = pendingDelete,
    archived = archived,
    errorMessage = errorMessage,
    completedAtMillis = completedAtMillis,
)

internal fun Download.toEntity() = DownloadEntity(
    id = id,
    uid = uid,
    url = url,
    status = status.toEntity(),
    seen = seen,
    pendingDelete = pendingDelete,
    archived = archived,
    errorMessage = errorMessage,
    completedAtMillis = completedAtMillis,
)
