package org.strigate.ferrot.domain.usecase.youtubedl_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.FINAL_OUTPUT_PATH_PREFIX
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.extractFinalOutputFilePath
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.finalOutputPathPrintTemplate

class YoutubeDlOutputPathTest {
    @Test
    fun finalOutputPathPrintTemplate_usesAfterMoveAndUniquePrefix() {
        assertEquals(
            "after_move:${FINAL_OUTPUT_PATH_PREFIX}%(filepath)s",
            finalOutputPathPrintTemplate(),
        )
    }

    @Test
    fun extractFinalOutputFilePath_returnsLastReportedPath() {
        val output = """
            [download] 12.3% of 10.00MiB at 1.00MiB/s ETA 00:12
            ${FINAL_OUTPUT_PATH_PREFIX}/data/user/0/org.strigate.ferrot/files/downloads/uid/video.mp4
            ${FINAL_OUTPUT_PATH_PREFIX}/data/user/0/org.strigate.ferrot/files/downloads/uid/audio.mp3
        """.trimIndent()

        assertEquals(
            "/data/user/0/org.strigate.ferrot/files/downloads/uid/audio.mp3",
            extractFinalOutputFilePath(output),
        )
    }

    @Test
    fun extractFinalOutputFilePath_returnsNull_whenMarkerMissing() {
        assertNull(extractFinalOutputFilePath("[download] 99.9%"))
    }

    @Test
    fun buildVideoDownloadRequest_includesAfterMovePathPrintOption() {
        val request = BuildVideoDownloadRequestUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            qualityProfile = QualityProfile.MAX,
            noProgress = false,
        )

        val command = request.buildCommand()
        assertEquals(true, command.contains("--print"))
        assertEquals(true, command.contains(finalOutputPathPrintTemplate()))
    }

    @Test
    fun buildAudioDownloadRequest_includesAfterMovePathPrintOption() {
        val request = BuildAudioDownloadRequestUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            noProgress = false,
        )

        val command = request.buildCommand()
        assertEquals(true, command.contains("--print"))
        assertEquals(true, command.contains(finalOutputPathPrintTemplate()))
    }

    @Test
    fun buildThumbnailRequest_usesIdBasedTemplate() {
        val request = BuildThumbnailRequestUseCase().invoke(
            url = "https://example.com/video",
            outputDir = java.io.File("/tmp"),
            convertToJpg = true,
        )

        val command = request.buildCommand()
        assertEquals(true, command.contains("-o"))
        assertEquals(true, command.contains("/tmp/%(id)s.%(ext)s"))
    }
}
