package org.strigate.ferrot.data.local.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "cookie_set_domain",
    foreignKeys = [
        ForeignKey(
            entity = CookieSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["cookieSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("cookieSetId"),
        Index("domain"),
        Index(value = ["cookieSetId", "domain"], unique = true),
    ],
)
data class CookieSetDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cookieSetId: Long,
    val domain: String,
    val includeSubdomains: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
