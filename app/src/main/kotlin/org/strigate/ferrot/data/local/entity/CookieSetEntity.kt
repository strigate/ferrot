package org.strigate.ferrot.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cookie_set",
    indices = [
        Index("updatedAtMillis"),
    ],
)
data class CookieSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val source: String,
    val cookieFilePath: String,
    val userAgent: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastUsedAtMillis: Long? = null,
)
