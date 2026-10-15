package org.strigate.ferrot.data.local.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class CookieSetWithDomainsEntity(
    @Embedded val cookieSet: CookieSetEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["cookieSetId"],
    )
    val domains: List<CookieSetDomainEntity>,
)
