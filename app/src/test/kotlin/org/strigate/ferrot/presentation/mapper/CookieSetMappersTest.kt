package org.strigate.ferrot.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.presentation.model.CookieSetSourceUiData

class CookieSetMappersTest {
    @Test
    fun toUiData_mapsSourceAndSortsDomains() {
        val result = CookieSetWithDomains(
            cookieSet = CookieSet(
                id = 7L,
                name = "browser",
                source = CookieSetSource.WEBVIEW,
                cookieFilePath = "/cookies.txt",
            ),
            domains = listOf(
                CookieSetDomain(cookieSetId = 7L, domain = "z.example", includeSubdomains = false),
                CookieSetDomain(cookieSetId = 7L, domain = "a.example"),
            ),
        ).toUiData()

        assertEquals(7L, result.id)
        assertEquals("browser", result.name)
        assertEquals(CookieSetSourceUiData.WEBVIEW, result.source)
        assertEquals(listOf("a.example", "z.example"), result.domains.map { it.domain })
        assertEquals(listOf(true, false), result.domains.map { it.includeSubdomains })
    }

    @Test
    fun toUiData_mapsImportedFileSource() {
        val result = CookieSetWithDomains(
            cookieSet = CookieSet(
                name = "file",
                source = CookieSetSource.IMPORTED_FILE,
                cookieFilePath = "/cookies.txt",
            ),
            domains = emptyList(),
        ).toUiData()

        assertEquals(CookieSetSourceUiData.IMPORTED_FILE, result.source)
    }
}
