package org.strigate.ferrot.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringTest {
    @Test
    fun extractFileName_returnsLastPathSegment() {
        assertEquals("video.mp4", "/tmp/media/video.mp4".extractFileName())
    }

    @Test
    fun extractFileName_returnsNull_whenPathEndsWithSlashOrIsBlank() {
        assertNull("/tmp/media/".extractFileName())
        assertNull("   ".extractFileName())
        assertNull("video.mp4".extractFileName())
    }

    @Test
    fun stripFileExtension_removesOnlyLastExtension() {
        assertEquals("archive.tar", "archive.tar.gz".stripFileExtension())
        assertEquals("README", "README".stripFileExtension())
    }

    @Test
    fun extractFileExtension_returnsUppercaseExtension() {
        assertEquals("MP4", "/tmp/media/video.mp4".extractFileExtension())
        assertEquals("GZ", "/tmp/archive.tar.gz".extractFileExtension())
        assertEquals("PNG", "/tmp/cover.PnG".extractFileExtension())
    }

    @Test
    fun extractFileExtension_returnsNull_whenNoUsableExtensionExists() {
        assertNull("/tmp/media/readme".extractFileExtension())
        assertNull("/tmp/media/".extractFileExtension())
    }

    @Test
    fun toSafeFileName_removesUnsafeCharacters_andCollapsesWhitespace() {
        val result = " https://example.com/ a:*?\"<>|#%  title  ".toSafeFileName()

        assertEquals("example.com a title", result)
    }

    @Test
    fun toSafeFileName_returnsSanitizedValue_whenWithinLimit() {
        assertEquals("simple name", "simple   name".toSafeFileName(maxBytes = 40))
    }

    @Test
    fun toSafeFileName_truncatesWithoutBreakingUtf8Characters() {
        val result = "éééé".toSafeFileName(maxBytes = 5)

        assertEquals("éé", result)
    }

    @Test
    fun toSafeFileName_returnsEmptyString_whenOnlyUnsafeCharactersRemain() {
        assertEquals("", "https://#%/:*?\"<>|   ".toSafeFileName())
    }

    @Test
    fun toSafeFileName_keepsValue_whenByteCountMatchesLimitExactly() {
        assertEquals("éé", "éé".toSafeFileName(maxBytes = 4))
    }

    @Test
    fun toSafeFileName_trimsTrailingSpace_afterTruncation() {
        assertEquals("abc", "abc def".toSafeFileName(maxBytes = 4))
    }

    @Test
    fun guessMimeType_mapsKnownTypesAndFallsBackToWildcard() {
        assertEquals("video/*", "movie.webm".guessMimeType())
        assertEquals("audio/*", "track.m4a".guessMimeType())
        assertEquals("image/*", "cover.jpeg".guessMimeType())
        assertEquals("application/pdf", "document.pdf".guessMimeType())
        assertEquals("text/plain", "notes.txt".guessMimeType())
        assertEquals("*/*", "archive.bin".guessMimeType())
        assertEquals("*/*", "README".guessMimeType())
        assertEquals("video/*", "movie.MOV".guessMimeType())
    }
}
