package org.strigate.ferrot.cookies

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class CookieSetDomainParserTest {
    private val parser = CookieSetDomainParser()

    @Test
    fun parseDomainList_normalizesCommaSeparatedDomains() {
        val result = parser.parseDomainList(
            rawDomains = "https://X.com/login, .twitter.com, invalid",
            includeSubdomains = true,
        )

        assertEquals(
            listOf(
                ParsedCookieDomain("x.com", includeSubdomains = true),
                ParsedCookieDomain("twitter.com", includeSubdomains = true),
            ),
            result,
        )
    }

    @Test
    fun parseNetscapeDomains_readsNormalAndHttpOnlyCookieDomains() {
        val file = Files.createTempFile("cookies", ".txt").toFile().apply {
            writeText(
                """
                # Netscape HTTP Cookie File
                .x.com	TRUE	/	TRUE	0	auth	one
                #HttpOnly_.instagram.com	TRUE	/	TRUE	0	sessionid	two
                twitter.com	FALSE	/	TRUE	0	ct0	three
                """.trimIndent()
            )
        }

        val result = parser.parseNetscapeDomains(file)

        assertEquals(
            listOf(
                ParsedCookieDomain("x.com", includeSubdomains = true),
                ParsedCookieDomain("instagram.com", includeSubdomains = true),
                ParsedCookieDomain("twitter.com", includeSubdomains = false),
            ),
            result,
        )
    }
}
