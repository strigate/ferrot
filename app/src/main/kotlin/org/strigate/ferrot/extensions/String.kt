package org.strigate.ferrot.extensions

import java.util.Locale

fun String.extractFileName(): String? {
    return substringAfterLast('/', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
}

fun String.stripFileExtension(): String {
    return substringBeforeLast('.', missingDelimiterValue = this)
}

fun String.extractFileExtension(): String? {
    return extractFileName()
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.ROOT)
}

fun String.toSafeFileName(maxBytes: Int = 120): String {
    val sanitized = this
        .replace(Regex("""https?://"""), "")
        .replace(Regex("""[\\/:*?"<>|]"""), "")
        .replace(Regex("""[#%]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    val byteArray = sanitized.toByteArray(Charsets.UTF_8)
    if (byteArray.size <= maxBytes) {
        return sanitized
    }
    var end = maxBytes.coerceAtLeast(0)
    while (end > 0 && (byteArray[end].toInt() and 0xC0) == 0x80) {
        end--
    }
    return String(byteArray, 0, end, Charsets.UTF_8).trim()
}

fun String.guessMimeType(): String {
    val extension = substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp4", "m4v", "mov", "webm" -> "video/*"
        "mp3", "m4a", "aac", "opus" -> "audio/*"
        "jpg", "jpeg", "png", "webp" -> "image/*"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        else -> "*/*"
    }
}
