package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.presentation.model.CookieSetDomainUiData
import org.strigate.ferrot.presentation.model.CookieSetSourceUiData
import org.strigate.ferrot.presentation.model.CookieSetUiData

fun CookieSetWithDomains.toUiData() = CookieSetUiData(
    id = cookieSet.id,
    name = cookieSet.name,
    source = cookieSet.source.toUiData(),
    domains = domains
        .sortedBy { it.domain }
        .map { domain ->
            CookieSetDomainUiData(
                domain = domain.domain,
                includeSubdomains = domain.includeSubdomains,
            )
        },
)

private fun CookieSetSource.toUiData() = when (this) {
    CookieSetSource.IMPORTED_FILE -> CookieSetSourceUiData.IMPORTED_FILE
    CookieSetSource.WEBVIEW -> CookieSetSourceUiData.WEBVIEW
}
