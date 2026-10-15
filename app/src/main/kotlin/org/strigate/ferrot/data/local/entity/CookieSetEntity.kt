package org.strigate.ferrot.data.local.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
