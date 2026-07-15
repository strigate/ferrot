package org.strigate.ferrot.cookies

import javax.inject.Inject

class CookieHeaderFileBuilder @Inject constructor() {
    fun build(domains: List<ParsedCookieDomain>, rawCookieHeader: String): String? {
        val cookies = parseCookieHeader(rawCookieHeader)
        if (domains.isEmpty() || cookies.isEmpty()) {
            return null
        }

        return buildString {
            appendLine("# Netscape HTTP Cookie File")
            domains.forEach { domain ->
                cookies.forEach { cookie ->
                    appendLine(
                        listOf(
                            domain.netscapeDomain(),
                            if (domain.includeSubdomains) "TRUE" else "FALSE",
                            "/",
                            "TRUE",
                            SESSION_COOKIE_EXPIRES,
                            cookie.name,
                            cookie.value,
                        ).joinToString("\t")
                    )
                }
            }
        }
    }

    fun parseCookieHeader(rawCookieHeader: String): List<CookiePair> {
        val normalized = rawCookieHeader
            .trim()
            .removePrefix("Cookie:")
            .removePrefix("cookie:")
            .trim()

        return normalized
            .split(';')
            .mapNotNull { part ->
                val name = part.substringBefore('=', missingDelimiterValue = "").trim()
                val value = part.substringAfter('=', missingDelimiterValue = "").trim()
                if (name.isBlank() || value.isBlank() || name.lowercase() in RESERVED_ATTRIBUTES) {
                    return@mapNotNull null
                }
                CookiePair(
                    name = name.sanitizeCookieField(),
                    value = value.sanitizeCookieField(),
                )
            }
            .distinctBy { it.name }
    }

    private fun ParsedCookieDomain.netscapeDomain(): String {
        return if (includeSubdomains) ".$domain" else domain
    }

    private fun String.sanitizeCookieField(): String {
        return replace('\t', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim()
    }

    companion object {
        private const val SESSION_COOKIE_EXPIRES = ""

        private val RESERVED_ATTRIBUTES = setOf(
            "domain",
            "expires",
            "httponly",
            "max-age",
            "path",
            "samesite",
            "secure",
        )
    }
}

data class CookiePair(
    val name: String,
    val value: String,
)
