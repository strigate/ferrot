package org.strigate.ferrot.domain.usecase.youtubedl_android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BuildThumbnailRequestUseCaseTest {
    private val useCase = BuildThumbnailRequestUseCase()

    @Test
    fun invoke_buildsRequest_withJpgConversionByDefault() {
        val outputDir = Files.createTempDirectory("thumb-request").toFile()
        val request = useCase(
            url = "https://example.com/video",
            outputDir = outputDir,
            videoId = "abc:123 test",
        )

        assertEquals(
            File(outputDir, "thumb_abc123 test.%(ext)s").absolutePath,
            request.getOption("-o"),
        )
        assertTrue(request.hasOption("--restrict-filenames"))
        assertTrue(request.hasOption("--skip-download"))
        assertTrue(request.hasOption("--write-thumbnail"))
        assertEquals("jpg", request.getOption("--convert-thumbnails"))
        assertTrue(request.hasOption("--no-progress"))
    }

    @Test
    fun invoke_skipsConversion_whenDisabled() {
        val outputDir = Files.createTempDirectory("thumb-request-no-convert").toFile()
        val request = useCase(
            url = "https://example.com/video",
            outputDir = outputDir,
            videoId = "plain-id",
            convertToJpg = false,
        )

        assertFalse(request.hasOption("--convert-thumbnails"))
    }
}
