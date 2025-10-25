package org.strigate.ferrot.extensions

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
