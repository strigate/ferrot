package org.strigate.ferrot.cookies

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebViewCookieDomainResolverTest {
    private val resolver = WebViewCookieDomainResolver(CookieSetDomainParser())

    @Test
    fun invoke_normalizesHostAndRemovesRepeatedCommonPrefixes() {
        assertEquals("example.com", resolver("https://www.m.example.com/watch"))
    }

    @Test
    fun invoke_preservesOrdinarySubdomain() {
        assertEquals("media.example.com", resolver("https://media.example.com"))
    }

    @Test
    fun invoke_returnsNullForInvalidOrHostlessUrl() {
        assertNull(resolver("not a url"))
        assertNull(resolver("file:///tmp/cookies.txt"))
    }
}
