package org.strigate.ferrot.domain.model

data class CookieSet(
    val id: Long = 0L,
    val name: String,
    val source: CookieSetSource,
    val cookieFilePath: String,
    val userAgent: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val lastUsedAtMillis: Long? = null,
)
