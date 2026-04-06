package org.strigate.ferrot.domain.usecase.youtubedl_android.internal

internal const val FINAL_OUTPUT_PATH_PREFIX = "__FINAL_OUTPUT_PATH__:"

internal fun finalOutputPathPrintTemplate(): String {
    return "after_move:${FINAL_OUTPUT_PATH_PREFIX}%(filepath)s"
}

internal fun extractFinalOutputFilePath(output: String): String? {
    return output
        .lineSequence()
        .map { it.trim() }
        .filter { it.startsWith(FINAL_OUTPUT_PATH_PREFIX) }
        .map { it.removePrefix(FINAL_OUTPUT_PATH_PREFIX).trim() }
        .lastOrNull()
        ?.takeIf { it.isNotBlank() }
}
