package org.strigate.ferrot.data.mapper

import org.strigate.ferrot.data.local.entity.CookieSetDomainEntity
import org.strigate.ferrot.data.local.entity.CookieSetEntity
import org.strigate.ferrot.data.local.entity.CookieSetWithDomainsEntity
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains

internal fun CookieSetEntity.toDomain() = CookieSet(
    id = id,
    name = name,
    source = CookieSetSource.valueOf(source),
    cookieFilePath = cookieFilePath,
    userAgent = userAgent,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    lastUsedAtMillis = lastUsedAtMillis,
)

internal fun CookieSet.toEntity() = CookieSetEntity(
    id = id,
    name = name,
    source = source.name,
    cookieFilePath = cookieFilePath,
    userAgent = userAgent,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    lastUsedAtMillis = lastUsedAtMillis,
)

internal fun CookieSetDomainEntity.toDomain() = CookieSetDomain(
    id = id,
    cookieSetId = cookieSetId,
    domain = domain,
    includeSubdomains = includeSubdomains,
    createdAtMillis = createdAtMillis,
)

internal fun CookieSetDomain.toEntity() = CookieSetDomainEntity(
    id = id,
    cookieSetId = cookieSetId,
    domain = domain,
    includeSubdomains = includeSubdomains,
    createdAtMillis = createdAtMillis,
)

internal fun CookieSetWithDomainsEntity.toDomain() = CookieSetWithDomains(
    cookieSet = cookieSet.toDomain(),
    domains = domains.map { it.toDomain() },
)
