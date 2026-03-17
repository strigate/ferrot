package org.strigate.ferrot.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadWithMetadata

class DownloadWithMetadataMappersTest {
    @Test
    fun toUiData_prefersExpectedBytesBasedProgress_whenAvailable() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = 75L,
            expectedBytes = 50L,
            progressPercent = 10f,
        ).toUiData()

        assertEquals(1f, result.progressFraction)
    }

    @Test
    fun toUiData_fallsBackToProgressPercent_whenExpectedBytesAreUnavailable() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = 75L,
            expectedBytes = null,
            progressPercent = 25f,
        ).toUiData()

        assertEquals(0.25f, result.progressFraction)
    }

    @Test
    fun toUiData_returnsNullProgress_whenNoUsableProgressExists() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = -1L,
            expectedBytes = null,
            progressPercent = -1f,
        ).toUiData()

        assertNull(result.progressFraction)
    }

    @Test
    fun toUiData_fallsBackToProgressPercent_whenExpectedBytesIsZero() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = 50L,
            expectedBytes = 0L,
            progressPercent = 40f,
        ).toUiData()

        assertEquals(0.4f, result.progressFraction)
    }

    @Test
    fun toUiData_fallsBackToProgressPercent_whenBytesDownloadedIsNegative() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = -5L,
            expectedBytes = 100L,
            progressPercent = 60f,
        ).toUiData()

        assertEquals(0.6f, result.progressFraction)
    }

    @Test
    fun toUiData_clampsProgressPercentAboveHundred() {
        val result = sampleDownloadWithMetadata(
            bytesDownloaded = -1L,
            expectedBytes = null,
            progressPercent = 150f,
        ).toUiData()

        assertEquals(1f, result.progressFraction)
    }

    private fun sampleDownloadWithMetadata(
        bytesDownloaded: Long,
        expectedBytes: Long?,
        progressPercent: Float,
    ) = DownloadWithMetadata(
        id = 1L,
        url = "https://example.com/video",
        title = "Title",
        thumbnailFilePath = null,
        status = DownloadStatus.DOWNLOADING,
        seen = false,
        progressPercent = progressPercent,
        etaSeconds = 10L,
        bytesDownloaded = bytesDownloaded,
        expectedBytes = expectedBytes,
        completedAtMillis = null,
    )
}
