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

fun String.guessMimeType(): String {
    val ext = substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp4", "m4v", "mov", "webm" -> "video/*"
        "mp3", "m4a", "aac", "opus" -> "audio/*"
        "jpg", "jpeg", "png", "webp" -> "image/*"
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        else -> "*/*"
    }
}
