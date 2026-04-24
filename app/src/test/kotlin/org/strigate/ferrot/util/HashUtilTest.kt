package org.strigate.ferrot.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files
import java.util.Locale

class HashUtilTest {
    @Test
    fun sha256_returnsNull_whenPathDoesNotPointToAFile() {
        assertNull(sha256("/definitely/missing/file.txt"))
    }

    @Test
    fun sha256_returnsExpectedDigest_forKnownFileContents() {
        val file = Files.createTempFile("hash-util-test", ".txt")
        Files.write(file, "hello world".toByteArray())

        val result = sha256(file.toString())
        assertEquals(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            result,
        )
    }

    @Test
    fun sha256_usesStableAsciiHex_evenWhenDefaultLocaleChanges() {
        val file = Files.createTempFile("hash-util-locale-test", ".txt")
        Files.write(file, "hello world".toByteArray())
        val originalLocale = Locale.getDefault()

        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals(
                "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
                sha256(file.toString()),
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
