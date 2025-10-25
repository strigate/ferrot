package org.strigate.ferrot.presentation.util

import java.util.Locale

object UiFormatter {
    fun formatBytes(byteCount: Long): String {
        if (byteCount <= 0) {
            return "0 B"
        }
        val sizeUnits = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = byteCount.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < sizeUnits.lastIndex) {
            size /= 1024.0
            unitIndex++
        }
        return String.format(Locale.getDefault(), "%.1f %s", size, sizeUnits[unitIndex])
    }

    fun formatEta(totalSeconds: Long?): String? {
        if (totalSeconds == null || totalSeconds <= 0) {
            return null
        }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
            else -> String.format(Locale.getDefault(), "%ds", seconds)
        }
    }
}
