package org.strigate.ferrot.cookies

import java.io.File
import java.util.Locale
import javax.inject.Inject

class CookieSetDomainParser @Inject constructor() {
    fun parseDomainList(rawDomains: String, includeSubdomains: Boolean): List<ParsedCookieDomain> {
        return rawDomains
            .split(',', '\n')
            .mapNotNull { rawDomain ->
                normalizeDomain(rawDomain)?.let { domain ->
                    ParsedCookieDomain(
                        domain = domain,
                        includeSubdomains = includeSubdomains,
                    )
                }
            }
            .distinctBy { it.domain }
    }

    fun parseNetscapeDomains(file: File): List<ParsedCookieDomain> {
        if (!file.exists()) {
            return emptyList()
        }
        return file
            .readLines()
            .mapNotNull(::parseNetscapeDomain)
            .distinctBy { it.domain }
    }

    fun normalizeDomain(rawDomain: String): String? {
        return rawDomain
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore('/')
            .substringBefore(':')
            .removePrefix("#HttpOnly_")
            .removePrefix(".")
            .trim()
            .lowercase(Locale.US)
            .takeIf { domain ->
                domain.isNotBlank() && domain.contains('.') && !domain.any(Char::isWhitespace)
            }
    }

    private fun parseNetscapeDomain(line: String): ParsedCookieDomain? {
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            return null
        }
        if (trimmed.startsWith("#") && !trimmed.startsWith("#HttpOnly_")) {
            return null
        }

        val columns = trimmed.split(Regex("\\s+"), limit = NETSCAPE_COLUMNS)
        if (columns.size < NETSCAPE_COLUMNS) {
            return null
        }
        val rawDomain = columns[0].removePrefix("#HttpOnly_")
        val includeSubdomains =
            columns[1].equals("TRUE", ignoreCase = true) || rawDomain.startsWith('.')
        val domain = normalizeDomain(rawDomain) ?: return null

        return ParsedCookieDomain(
            domain = domain,
            includeSubdomains = includeSubdomains,
        )
    }

    companion object {
        private const val NETSCAPE_COLUMNS = 7
    }
}

data class ParsedCookieDomain(
    val domain: String,
    val includeSubdomains: Boolean,
)
