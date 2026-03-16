package org.strigate.ferrot.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadVideo

class DownloadMappersTest {
    @Test
    fun toPageUiData_prefersMetadataTitle_andBuildsMediaUiData() {
        val result = sampleDownload().toPageUiData(
            video = DownloadVideo(
                downloadId = 1L,
                filePath = "/tmp/video-file.mp4",
                fileExtension = "",
                sha256 = null,
            ),
            audio = DownloadAudio(
                downloadId = 1L,
                filePath = "/tmp/audio-track.m4a",
                fileExtension = "",
            ),
            metadata = DownloadMetadata(
                downloadId = 1L,
                videoId = "abc",
                source = "youtube",
                title = "Real title",
                thumbnailFilePath = "/tmp/thumb.jpg",
                durationSeconds = 90,
            ),
            progress = DownloadProgress(
                downloadId = 1L,
                updatedAtMillis = 10L,
                progressPercent = 25f,
                bytesDownloaded = 100L,
                etaSeconds = 4L,
                expectedBytes = 400L,
            ),
        )

        assertEquals("Real title", result.metadata?.title)
        assertEquals("video-file.mp4", result.video?.fileName)
        assertEquals("MP4", result.video?.fileExtension)
        assertEquals("audio-track.m4a", result.audio?.fileName)
        assertEquals("M4A", result.audio?.fileExtension)
        assertEquals(0.25f, result.progress?.progressFraction)
    }

    @Test
    fun toPageUiData_fallsBackToDerivedVideoTitle_whenMetadataTitleIsBlank() {
        val result = sampleDownload(url = "https://example.com/fallback").toPageUiData(
            video = DownloadVideo(
                downloadId = 1L,
                filePath = "/tmp/episode-final.mp4",
                fileExtension = "MP4",
                sha256 = null,
            ),
            audio = null,
            metadata = DownloadMetadata(
                downloadId = 1L,
                videoId = null,
                source = null,
                title = " ",
                thumbnailFilePath = null,
                durationSeconds = null,
            ),
            progress = null,
        )

        assertEquals("episode-final", result.metadata?.title)
        assertNull(result.audio)
        assertNull(result.progress)
    }

    @Test
    fun toPageUiData_filtersBlankMediaPaths_andClampsInvalidProgress() {
        val result = sampleDownload().toPageUiData(
            video = DownloadVideo(
                downloadId = 1L,
                filePath = " ",
                fileExtension = "MP4",
                sha256 = null,
            ),
            audio = DownloadAudio(
                downloadId = 1L,
                filePath = "",
                fileExtension = "M4A",
            ),
            metadata = null,
            progress = DownloadProgress(
                downloadId = 1L,
                updatedAtMillis = 10L,
                progressPercent = Float.NaN,
                bytesDownloaded = 100L,
                etaSeconds = null,
                expectedBytes = null,
            ),
        )

        assertNull(result.video)
        assertNull(result.audio)
        assertNotNull(result.metadata)
        assertEquals(sampleDownload().url, result.metadata?.title)
        assertEquals(0f, result.progress?.progressFraction)
    }

    @Test
    fun toPageUiData_fallsBackToDerivedAudioTitle_whenVideoTitleIsUnavailable() {
        val result = sampleDownload(url = "https://example.com/audio-only").toPageUiData(
            video = null,
            audio = DownloadAudio(
                downloadId = 1L,
                filePath = "/tmp/podcast-episode.opus",
                fileExtension = "OPUS",
            ),
            metadata = DownloadMetadata(
                downloadId = 1L,
                videoId = null,
                source = null,
                title = "",
                thumbnailFilePath = null,
                durationSeconds = null,
            ),
            progress = null,
        )

        assertEquals("podcast-episode", result.metadata?.title)
        assertEquals("OPUS", result.audio?.fileExtension)
    }

    @Test
    fun toPageUiData_usesExplicitVideoExtension_andClampsProgressAboveHundred() {
        val result = sampleDownload().toPageUiData(
            video = DownloadVideo(
                downloadId = 1L,
                filePath = "/tmp/archive.bin",
                fileExtension = "MKV",
                sha256 = null,
            ),
            audio = null,
            metadata = null,
            progress = DownloadProgress(
                downloadId = 1L,
                updatedAtMillis = 10L,
                progressPercent = 150f,
                bytesDownloaded = 100L,
                etaSeconds = 1L,
                expectedBytes = 100L,
            ),
        )

        assertEquals("MKV", result.video?.fileExtension)
        assertEquals(1f, result.progress?.progressFraction)
    }

    @Test
    fun toPageUiData_usesEmptyExtension_whenFileNameHasNoExtension_andClampsNegativeProgress() {
        val result = sampleDownload().toPageUiData(
            video = DownloadVideo(
                downloadId = 1L,
                filePath = "/tmp/readme",
                fileExtension = "",
                sha256 = null,
            ),
            audio = null,
            metadata = null,
            progress = DownloadProgress(
                downloadId = 1L,
                updatedAtMillis = 10L,
                progressPercent = -25f,
                bytesDownloaded = 0L,
                etaSeconds = null,
                expectedBytes = null,
            ),
        )

        assertEquals("", result.video?.fileExtension)
        assertEquals("readme", result.video?.fileName)
        assertEquals(0f, result.progress?.progressFraction)
    }

    private fun sampleDownload(url: String = "https://example.com/video") = Download(
        id = 1L,
        uid = "uid-1",
        url = url,
        status = DownloadStatus.DOWNLOADING,
        seen = false,
        errorMessage = null,
        completedAtMillis = null,
    )
}
