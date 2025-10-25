package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.DownloadStatus as EntityStatus
import org.strigate.ferrot.domain.model.DownloadStatus as DomainStatus

internal fun EntityStatus.toDomain() = when (this) {
    EntityStatus.QUEUED -> DomainStatus.QUEUED
    EntityStatus.WAITING_FOR_NETWORK -> DomainStatus.WAITING_FOR_NETWORK
    EntityStatus.WAITING_FOR_WIFI -> DomainStatus.WAITING_FOR_WIFI
    EntityStatus.METADATA -> DomainStatus.METADATA
    EntityStatus.DOWNLOADING -> DomainStatus.DOWNLOADING
    EntityStatus.PAUSED -> DomainStatus.PAUSED
    EntityStatus.COMPLETED -> DomainStatus.COMPLETED
    EntityStatus.FAILED -> DomainStatus.FAILED
    EntityStatus.STOPPED -> DomainStatus.STOPPED
}

internal fun DomainStatus.toEntity() = when (this) {
    DomainStatus.QUEUED -> EntityStatus.QUEUED
    DomainStatus.WAITING_FOR_NETWORK -> EntityStatus.WAITING_FOR_NETWORK
    DomainStatus.WAITING_FOR_WIFI -> EntityStatus.WAITING_FOR_WIFI
    DomainStatus.METADATA -> EntityStatus.METADATA
    DomainStatus.DOWNLOADING -> EntityStatus.DOWNLOADING
    DomainStatus.PAUSED -> EntityStatus.PAUSED
    DomainStatus.COMPLETED -> EntityStatus.COMPLETED
    DomainStatus.FAILED -> EntityStatus.FAILED
    DomainStatus.STOPPED -> EntityStatus.STOPPED
}
