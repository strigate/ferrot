package org.strigate.ferrot.presentation.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import org.strigate.ferrot.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
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

    fun formatLastCheckedTime(context: Context, millis: Long): String {
        if (millis <= 0L) {
            return context.getString(R.string.never)
        }
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()

        val dateFormat = DateFormat.getMediumDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        val exact = "${dateFormat.format(Date(millis))}, ${timeFormat.format(Date(millis))}"
        return "$relativeTime ($exact)"
    }

    fun formatDuration(seconds: Int?): String? {
        if (seconds == null || seconds <= 0) {
            return null
        }
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
        }
    }

    fun formatCompletedAtTime(
        context: Context,
        millis: Long,
    ): String {
        val zoned = Instant
            .ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())

        val locale = Locale.getDefault()
        val is24Hour = DateFormat.is24HourFormat(context)
        val timePattern = if (is24Hour) {
            "HH:mm"
        } else {
            "hh:mm a"
        }
        val timeFormatter = DateTimeFormatter.ofPattern(timePattern, locale)
        val bestDatePattern = DateFormat.getBestDateTimePattern(locale, "EEE, MMM d")
        val dateFormatter = DateTimeFormatter.ofPattern(bestDatePattern, locale)
        val today = LocalDate.now()
        return if (zoned.toLocalDate() == today) {
            zoned.format(timeFormatter)
        } else {
            "${zoned.format(dateFormatter)} ${zoned.format(timeFormatter)}"
        }
    }

    fun formatCompletedAtDetail(
        context: Context,
        millis: Long,
    ): String {
        val zoned = Instant
            .ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())

        val is24Hour = DateFormat.is24HourFormat(context)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
        val timeFormatter = DateTimeFormatter.ofPattern(
            if (is24Hour) "HH:mm" else "hh:mm a",
            Locale.getDefault(),
        )
        return "${zoned.format(dateFormatter)} ${zoned.format(timeFormatter)}"
    }
}
