package org.strigate.ferrot.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CookieSetWithDomainsEntity(
    @Embedded val cookieSet: CookieSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cookieSetId",
    )
    val domains: List<CookieSetDomainEntity>,
)
