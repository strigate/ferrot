package org.strigate.ferrot.cookies

import java.net.URI
import javax.inject.Inject

class WebViewCookieDomainResolver @Inject constructor(
    private val cookieSetDomainParser: CookieSetDomainParser,
) {
    operator fun invoke(url: String): String {
        val host = runCatching { URI(url).host }
            .getOrNull()
        val normalizedHost = requireNotNull(cookieSetDomainParser.normalizeDomain(host.orEmpty())) {
            "Could not determine cookie domain"
        }
        return normalizedHost.withoutCommonSitePrefix()
    }

    private fun String.withoutCommonSitePrefix(): String {
        var domain = this
        while (true) {
            val stripped = COMMON_SITE_PREFIXES.firstNotNullOfOrNull { prefix ->
                domain
                    .removePrefix("$prefix.")
                    .takeIf { candidate ->
                        candidate != domain && cookieSetDomainParser.normalizeDomain(candidate) != null
                    }
            } ?: return domain
            domain = stripped
        }
    }

    companion object {
        private val COMMON_SITE_PREFIXES = listOf(
            "www",
            "m",
            "mobile",
            "account",
            "accounts",
            "sso",
        )
    }
}
