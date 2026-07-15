package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
