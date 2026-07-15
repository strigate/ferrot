package org.strigate.ferrot.domain.model

data class CookieSetWithDomains(
    val cookieSet: CookieSet,
    val domains: List<CookieSetDomain>,
)
