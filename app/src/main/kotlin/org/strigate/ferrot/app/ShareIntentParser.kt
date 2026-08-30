package org.strigate.ferrot.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import java.net.IDN
import java.net.URI

object ShareIntentParser {
    private val HTTP_URL_REGEX = Regex(
        pattern = """https?://[^\s<>"]+""",
        option = RegexOption.IGNORE_CASE,
    )

    fun extractUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) {
            return null
        }
        firstUrlFromTextExtra(intent)?.let { return it }
        getStreamUri(intent)?.let { normalizeHttpUri(it)?.let { url -> return url } }
        firstUrlFromClipData(intent.clipData)?.let { return it }
        return normalizeHttpUri(intent.data)
    }

    fun findFirstHttpUrl(text: String): String? {
        return findHttpUrls(text).firstOrNull()
    }

    fun normalizeHttpUrl(value: String): String? {
        val candidate = value.trim()
        return validateHttpUrl(candidate)
    }

    private fun normalizeTextHttpUrl(value: String): String? {
        var candidate = value.trim()
        while (candidate.isNotEmpty() && shouldTrimTrailingCharacter(candidate)) {
            candidate = candidate.dropLast(1)
        }
        return validateHttpUrl(candidate)
    }

    private fun validateHttpUrl(candidate: String): String? {
        if (candidate.isEmpty()) {
            return null
        }
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        if (!uri.scheme.equals("http", ignoreCase = true) &&
            !uri.scheme.equals("https", ignoreCase = true)
        ) {
            return null
        }
        if (uri.port > 65535) {
            return null
        }
        if (!hasValidHost(uri)) {
            return null
        }
        return candidate
    }

    private fun firstUrlFromTextExtra(intent: Intent): String? {
        val text = intent.extras?.getCharSequence(Intent.EXTRA_TEXT)?.toString()
        return text?.let(::findFirstHttpUrl)
    }

    private fun getStreamUri(intent: Intent): Uri? {
        return IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
    }

    private fun firstUrlFromClipData(clipData: ClipData?): String? {
        if (clipData == null) {
            return null
        }
        for (index in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(index)
            item.text?.let { text ->
                findFirstHttpUrl(text.toString())?.let { return it }
            }
            normalizeHttpUri(item.uri)?.let { return it }
        }
        return null
    }

    private fun findHttpUrls(text: String): List<String> {
        return HTTP_URL_REGEX.findAll(text)
            .mapNotNull { match -> normalizeTextHttpUrl(match.value) }
            .toList()
    }

    private fun normalizeHttpUri(uri: Uri?): String? {
        return uri?.let { normalizeHttpUrl(it.toString()) }
    }

    private fun hasValidHost(uri: URI): Boolean {
        if (!uri.host.isNullOrBlank()) {
            return true
        }

        val authority = uri.rawAuthority ?: return false
        val hostAndPort = authority.substringAfterLast('@')
        if (hostAndPort.startsWith('[')) {
            return false
        }
        val portSeparator = hostAndPort.lastIndexOf(':')
        val host = if (portSeparator >= 0) {
            val port = hostAndPort.substring(portSeparator + 1)
            if (port.isEmpty() || port.any { !it.isDigit() } || port.toIntOrNull() !in 0..65535) {
                return false
            }
            hostAndPort.substring(0, portSeparator)
        } else {
            hostAndPort
        }
        if (host.isEmpty()) {
            return false
        }
        return runCatching {
            IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES)
        }.getOrNull()?.isNotEmpty() == true
    }

    private fun shouldTrimTrailingCharacter(value: String): Boolean {
        return when (value.last()) {
            '.', ',', '?', '#', '"' -> true
            ')' -> value.count { it == ')' } > value.count { it == '(' }
            ']' -> value.count { it == ']' } > value.count { it == '[' }
            '}' -> value.count { it == '}' } > value.count { it == '{' }
            else -> false
        }
    }
}
