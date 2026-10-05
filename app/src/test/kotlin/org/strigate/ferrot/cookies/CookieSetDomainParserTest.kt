package org.strigate.ferrot.cookies

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CookieSetDomainParserTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

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
        val file = temporaryFolder.newFile("cookies.txt").apply {
            writeText(
                text = """
                # Netscape HTTP Cookie File
                .x.com	TRUE	/	TRUE	0	auth	one
                #HttpOnly_.instagram.com	TRUE	/	TRUE	0	sessionid	two
                twitter.com	FALSE	/	TRUE	0	ct0	three
                """.trimIndent(),
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

    @Test
    fun parseNetscapeDomains_mergesDuplicateDomainsWithSubdomainsEnabled() {
        val file = temporaryFolder.newFile("cookies.txt").apply {
            writeText(
                text = """
                # Netscape HTTP Cookie File
                example.com	FALSE	/	TRUE	0	auth	one
                .example.com	TRUE	/	TRUE	0	ct0	two
                """.trimIndent(),
            )
        }

        val result = parser.parseNetscapeDomains(file)

        assertEquals(
            listOf(ParsedCookieDomain("example.com", includeSubdomains = true)),
            result,
        )
    }
}
