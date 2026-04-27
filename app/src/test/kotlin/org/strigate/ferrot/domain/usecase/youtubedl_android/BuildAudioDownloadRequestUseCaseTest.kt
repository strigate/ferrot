package org.strigate.ferrot.domain.usecase.youtubedl_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildAudioDownloadRequestUseCaseTest {
    private val useCase = BuildAudioDownloadRequestUseCase()

    @Test
    fun invoke_buildsProgressRequest() {
        val request = useCase(
            url = "https://example.com/audio",
            template = "/tmp/%(title)s.%(ext)s",
            noProgress = false,
        )

        assertEquals("ba/b", request.getOption("-f"))
        assertEquals("/tmp/%(title)s.%(ext)s", request.getOption("-o"))
        assertTrue(request.hasOption("--extract-audio"))
        assertEquals("mp3", request.getOption("--audio-format"))
        assertEquals("0", request.getOption("--audio-quality"))
        assertTrue(request.hasOption("--no-overwrites"))
        assertTrue(request.hasOption("--no-post-overwrites"))
        assertTrue(request.hasOption("--newline"))
        assertFalse(request.hasOption("--no-progress"))
    }

    @Test
    fun invoke_addsPrintToFile_andGetFilename_whenRequested() {
        val request = useCase(
            url = "https://example.com/audio",
            template = "/tmp/%(title)s.%(ext)s",
            noProgress = true,
            outputPathFilePath = "/tmp/audio.txt",
        )

        val command = request.buildCommand()

        assertTrue(request.hasOption("--no-progress"))
        assertTrue(request.hasOption("--get-filename"))
        assertFalse(request.hasOption("--print"))
        assertTrue(
            command.containsAll(
                listOf("--print-to-file", "after_move:%(filepath)s", "/tmp/audio.txt"),
            ),
        )
    }

    @Test
    fun invoke_printsFilename_whenRequested() {
        val request = useCase(
            url = "https://example.com/audio",
            template = "/tmp/%(title)s.%(ext)s",
            noProgress = true,
            printFilename = true,
        )

        assertEquals("filename", request.getOption("--print"))
        assertFalse(request.hasOption("--get-filename"))
    }
}
