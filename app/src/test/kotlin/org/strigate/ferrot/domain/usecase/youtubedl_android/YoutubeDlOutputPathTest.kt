package org.strigate.ferrot.domain.usecase.youtubedl_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.finalOutputPathTemplate
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.readFinalOutputFilePath
import java.io.File

class YoutubeDlOutputPathTest {
    @Test
    fun buildVideoDownloadRequest_includesPrintToFileOption_whenPathProvided() {
        val request = BuildVideoDownloadRequestUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            qualityProfile = QualityProfile.MAX,
            noProgress = false,
            outputPathFilePath = "/tmp/video-output-path.txt",
        )

        val command = request.buildCommand()
        assertTrue(command.contains("--print-to-file"))
        assertTrue(command.contains(finalOutputPathTemplate()))
        assertTrue(command.contains("/tmp/video-output-path.txt"))
    }

    @Test
    fun buildAudioDownloadRequest_includesPrintToFileOption_whenPathProvided() {
        val request = BuildAudioDownloadRequestUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            noProgress = false,
            outputPathFilePath = "/tmp/audio-output-path.txt",
        )

        val command = request.buildCommand()
        assertTrue(command.contains("--print-to-file"))
        assertTrue(command.contains(finalOutputPathTemplate()))
        assertTrue(command.contains("/tmp/audio-output-path.txt"))
    }

    @Test
    fun readFinalOutputFilePath_returnsLastNonBlankLine() {
        val tempFile = File.createTempFile("ferrot-output-path", ".txt")
        tempFile.writeText(
            """

            /tmp/video.mp4
            /tmp/audio.mp3
            """.trimIndent()
        )

        assertEquals("/tmp/audio.mp3", readFinalOutputFilePath(tempFile))
        tempFile.delete()
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
