package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity
import org.strigate.ferrot.domain.model.AvailableUpdate

internal fun AvailableUpdateEntity.toDomain() = AvailableUpdate(
    tag = tag,
    localFilePath = localFilePath,
)

internal fun AvailableUpdate.toEntity() = AvailableUpdateEntity(
    id = 0,
    tag = tag,
    localFilePath = localFilePath,
)
