package org.strigate.ferrot.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkScheduleTest {
    @Test
    fun calculateDailyInitialDelayMillis_returnsSameDayDelay_whenBeforeTargetHour() {
        val zoneId = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 3, 4, 2, 15, 0, 0, zoneId)

        val result = calculateDailyInitialDelayMillis(
            targetHour = 3,
            zoneId = zoneId,
            now = now,
        )
        assertEquals(45 * 60 * 1000L, result)
    }

    @Test
    fun calculateDailyInitialDelayMillis_returnsNextDayDelay_whenAfterTargetHour() {
        val zoneId = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 3, 4, 5, 30, 0, 0, zoneId)

        val result = calculateDailyInitialDelayMillis(
            targetHour = 4,
            zoneId = zoneId,
            now = now,
        )
        assertEquals((22 * 60 + 30) * 60 * 1000L, result)
    }

    @Test
    fun calculateDailyInitialDelayMillis_returns24Hours_whenAtTargetHour() {
        val zoneId = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 3, 4, 3, 0, 0, 0, zoneId)

        val result = calculateDailyInitialDelayMillis(
            targetHour = 3,
            zoneId = zoneId,
            now = now,
        )
        assertEquals(24 * 60 * 60 * 1000L, result)
    }

    @Test
    fun calculateDailyTriggerAtMillis_returnsExpectedTimestamp_whenBeforeTargetHour() {
        val zoneId = ZoneId.of("UTC")
        val now = ZonedDateTime.of(2026, 3, 4, 2, 15, 0, 0, zoneId)

        val result = calculateDailyTriggerAtMillis(
            targetHour = 3,
            zoneId = zoneId,
            now = now,
        )

        assertEquals(
            ZonedDateTime.of(2026, 3, 4, 3, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            result,
        )
    }
}
