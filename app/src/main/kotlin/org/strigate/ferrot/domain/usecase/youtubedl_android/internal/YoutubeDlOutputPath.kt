package org.strigate.ferrot.domain.usecase.youtubedl_android.internal

import java.io.File

internal fun finalOutputPathTemplate(): String = "after_move:%(filepath)s"

internal fun readFinalOutputFilePath(file: File): String? {
    if (!file.exists()) {
        return null
    }
    return runCatching {
        file.readLines()
            .asReversed()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
