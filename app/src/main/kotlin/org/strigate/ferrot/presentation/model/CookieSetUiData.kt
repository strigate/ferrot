package org.strigate.ferrot.presentation.model

data class CookieSetUiData(
    val id: Long,
    val name: String,
    val source: CookieSetSourceUiData,
    val domains: List<CookieSetDomainUiData>,
)
