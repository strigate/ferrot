package org.strigate.ferrot.domain.model

data class CookieSetDomain(
    val id: Long = 0L,
    val cookieSetId: Long,
    val domain: String,
    val includeSubdomains: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
