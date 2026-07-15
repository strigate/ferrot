package org.strigate.ferrot.cookies

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieHeaderFileBuilderTest {
    private val builder = CookieHeaderFileBuilder()

    @Test
    fun build_writesNetscapeCookieFile_forEachDomainAndCookie() {
        val result = builder.build(
            domains = listOf(
                ParsedCookieDomain("x.com", includeSubdomains = true),
                ParsedCookieDomain("twitter.com", includeSubdomains = false),
            ),
            rawCookieHeader = "Cookie: auth=one; ct0=two",
        )

        assertTrue(result?.contains("# Netscape HTTP Cookie File") == true)
        assertTrue(result?.contains(".x.com\tTRUE\t/\tTRUE\t\tauth\tone") == true)
        assertTrue(result?.contains("twitter.com\tFALSE\t/\tTRUE\t\tct0\ttwo") == true)
    }

    @Test
    fun build_returnsNull_whenCookieHeaderHasNoCookies() {
        val result = builder.build(
            domains = listOf(ParsedCookieDomain("x.com", includeSubdomains = true)),
            rawCookieHeader = "Path=/; Secure; SameSite=Lax",
        )

        assertNull(result)
    }

    @Test
    fun parseCookieHeader_ignoresSetCookieAttributes() {
        val result = builder.parseCookieHeader(
            "session=abc; Path=/; Secure; SameSite=Lax; auth=def",
        )

        assertTrue(result.contains(CookiePair("session", "abc")))
        assertTrue(result.contains(CookiePair("auth", "def")))
        assertFalse(result.any { it.name == "Path" })
        assertFalse(result.any { it.name == "SameSite" })
    }
}
