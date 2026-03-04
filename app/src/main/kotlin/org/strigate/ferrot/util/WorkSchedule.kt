package org.strigate.ferrot.util

import java.time.Duration.between
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun calculateDailyInitialDelayMillis(
    targetHour: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
    now: ZonedDateTime = ZonedDateTime.now(zoneId),
): Long {
    val targetDateTime = now
        .withHour(targetHour)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    val firstRun = if (now.isBefore(targetDateTime)) {
        targetDateTime
    } else {
        targetDateTime.plusDays(1)
    }
    return between(now, firstRun).toMillis()
}
