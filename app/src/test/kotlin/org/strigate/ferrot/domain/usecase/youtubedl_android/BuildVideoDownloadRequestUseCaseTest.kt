package org.strigate.ferrot.domain.usecase.youtubedl_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.strigate.ferrot.domain.model.QualityProfile

class BuildVideoDownloadRequestUseCaseTest {
    private val useCase = BuildVideoDownloadRequestUseCase()

    @Test
    fun invoke_buildsProgressRequest_forMaxQuality() {
        val request = useCase(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            qualityProfile = QualityProfile.MAX,
            noProgress = false,
        )

        val command = request.buildCommand()

        assertEquals("bv*+ba/b", request.getOption("-f"))
        assertEquals("/tmp/%(title)s.%(ext)s", request.getOption("-o"))
        assertTrue(request.hasOption("--windows-filenames"))
        assertTrue(request.hasOption("--add-metadata"))
        assertTrue(request.hasOption("--newline"))
        assertFalse(request.hasOption("--no-progress"))
        assertFalse(request.hasOption("--merge-output-format"))
        assertEquals("aria2c", request.getOption("--external-downloader"))
        assertEquals("aria2c:-x16 -k1M", request.getOption("--external-downloader-args"))
        assertEquals("https://example.com/video", command.last())
    }

    @Test
    fun invoke_addsPrintToFile_andFilenamePrinting_whenRequested() {
        val request = useCase(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            qualityProfile = QualityProfile.CAP_2160,
            noProgress = true,
            outputPathFilePath = "/tmp/out.txt",
            printFilename = true,
        )

        val command = request.buildCommand()

        assertEquals("bv*[height<=2160]+ba/b", request.getOption("-f"))
        assertTrue(request.hasOption("--no-progress"))
        assertEquals("filename", request.getOption("--print"))
        assertFalse(request.hasOption("--get-filename"))
        assertTrue(
            command.containsAll(
                listOf("--print-to-file", "after_move:%(filepath)s", "/tmp/out.txt"),
            ),
        )
    }

    @Test
    fun invoke_forCompat2160_addsMergeFormat_andGetFilename() {
        val request = useCase(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            qualityProfile = QualityProfile.COMPAT_2160,
            noProgress = true,
        )

        assertEquals(
            "bv*[vcodec^=avc1][height<=2160]+ba[acodec^=mp4a]/b[ext=mp4]",
            request.getOption("-f"),
        )
        assertEquals("mp4", request.getOption("--merge-output-format"))
        assertTrue(request.hasOption("--get-filename"))
    }
}
