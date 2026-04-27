package org.strigate.ferrot.presentation.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.strigate.ferrot.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class UiFormatterTest {
    private lateinit var originalLocale: Locale
    private lateinit var originalZoneId: ZoneId

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalZoneId = ZoneId.systemDefault()
        Locale.setDefault(Locale.US)
    }


    @Test
    fun formatBytes_handlesZeroAndScaledValues() {
        assertEquals("0 B", UiFormatter.formatBytes(0))
        assertEquals("1.0 KB", UiFormatter.formatBytes(1024))
        assertEquals("1.5 KB", UiFormatter.formatBytes(1536))
        assertEquals("1.0 MB", UiFormatter.formatBytes(1024 * 1024))
    }

    @Test
    fun formatEta_formatsHoursMinutesAndSeconds() {
        assertNull(UiFormatter.formatEta(null))
        assertNull(UiFormatter.formatEta(0))
        assertEquals("59s", UiFormatter.formatEta(59))
        assertEquals("2m 5s", UiFormatter.formatEta(125))
        assertEquals("1h 1m", UiFormatter.formatEta(3665))
    }

    @Test
    fun formatDuration_formatsMinuteAndHourDurations() {
        assertNull(UiFormatter.formatDuration(null))
        assertNull(UiFormatter.formatDuration(0))
        assertEquals("0:59", UiFormatter.formatDuration(59))
        assertEquals("1:05", UiFormatter.formatDuration(65))
        assertEquals("1:01:05", UiFormatter.formatDuration(3665))
    }

    @Test
    fun formatLastCheckedTime_returnsNever_whenMillisIsNotPositive() {
        val context = mock(Context::class.java)
        `when`(context.getString(R.string.never))
            .thenReturn("Never")

        assertEquals("Never", UiFormatter.formatLastCheckedTime(context, 0L))
    }

    @Test
    fun formatLastCheckedTime_combinesRelativeAndExactFormats() {
        val context = mock(Context::class.java)
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val millis = 1234L

        mockStatic(DateUtils::class.java).use { dateUtilsMock ->
            mockStatic(DateFormat::class.java).use { dateFormatMock ->
                dateUtilsMock.`when`<CharSequence> {
                    DateUtils.getRelativeTimeSpanString(anyLong(), anyLong(), anyLong(), anyInt())
                }
                    .thenReturn("moments ago")
                dateFormatMock.`when`<java.text.DateFormat> { DateFormat.getMediumDateFormat(context) }
                    .thenReturn(dateFormat)
                dateFormatMock.`when`<java.text.DateFormat> { DateFormat.getTimeFormat(context) }
                    .thenReturn(timeFormat)

                val result = UiFormatter.formatLastCheckedTime(context, millis)
                val exact = "${dateFormat.format(java.util.Date(millis))}, ${
                    timeFormat.format(
                        java.util.Date(millis)
                    )
                }"
                assertEquals("moments ago ($exact)", result)
            }
        }
    }

    @Test
    fun formatCompletedAtTime_returnsTimeOnly_forTodayIn24HourFormat() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        val context = mock(Context::class.java)
        val millis = LocalDate.now(ZoneId.of("UTC"))
            .atStartOfDay(ZoneId.of("UTC"))
            .plusHours(13)
            .plusMinutes(5)
            .toInstant()
            .toEpochMilli()

        mockStatic(DateFormat::class.java).use { dateFormatMock ->
            dateFormatMock.`when`<Boolean> { DateFormat.is24HourFormat(context) }
                .thenReturn(true)
            dateFormatMock.`when`<String> {
                DateFormat.getBestDateTimePattern(
                    Locale.US,
                    "EEE, MMM d"
                )
            }
                .thenReturn("EEE, MMM d")

            val result = UiFormatter.formatCompletedAtTime(context, millis)
            assertEquals("13:05", result)
        }
    }

    @Test
    fun formatCompletedAtTime_includesDate_forNonTodayIn12HourFormat() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        val context = mock(Context::class.java)
        val millis = LocalDate.now(ZoneId.of("UTC"))
            .minusDays(1)
            .atStartOfDay(ZoneId.of("UTC"))
            .plusHours(1)
            .plusMinutes(30)
            .toInstant()
            .toEpochMilli()

        mockStatic(DateFormat::class.java).use { dateFormatMock ->
            dateFormatMock.`when`<Boolean> { DateFormat.is24HourFormat(context) }
                .thenReturn(false)
            dateFormatMock.`when`<String> {
                DateFormat.getBestDateTimePattern(
                    Locale.US,
                    "EEE, MMM d"
                )
            }
                .thenReturn("EEE, MMM d")

            val result = UiFormatter.formatCompletedAtTime(context, millis)
            assertTrue(result.contains("AM"))
            assertTrue(result.contains(","))
        }
    }

    @Test
    fun formatCompletedAtDetail_formats24HourDetail() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        val context = mock(Context::class.java)
        val millis = LocalDate.of(2024, 1, 2)
            .atStartOfDay(ZoneId.of("UTC"))
            .plusHours(21)
            .plusMinutes(45)
            .toInstant()
            .toEpochMilli()

        mockStatic(DateFormat::class.java).use { dateFormatMock ->
            dateFormatMock.`when`<Boolean> { DateFormat.is24HourFormat(context) }
                .thenReturn(true)

            val result = UiFormatter.formatCompletedAtDetail(context, millis)
            assertEquals("2024-01-02 21:45", result)
        }
    }

    @Test
    fun formatCompletedAtDetail_formats12HourDetail() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
        val context = mock(Context::class.java)
        val millis = LocalDate.of(2024, 1, 2)
            .atStartOfDay(ZoneId.of("UTC"))
            .plusHours(21)
            .plusMinutes(45)
            .toInstant()
            .toEpochMilli()

        mockStatic(DateFormat::class.java).use { dateFormatMock ->
            dateFormatMock.`when`<Boolean> { DateFormat.is24HourFormat(context) }
                .thenReturn(false)

            val result = UiFormatter.formatCompletedAtDetail(context, millis)
            assertEquals("2024-01-02 09:45 PM", result)
        }
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(originalZoneId))
    }

}
