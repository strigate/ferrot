package org.strigate.ferrot.app.integration

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.strigate.ferrot.app.provider.CookieSetPathProvider
import java.io.File
import java.nio.file.Files

class CookieFileStoreTest {
    @Test
    fun deleteStaleTempCookies_deletesOnlyOldTempFiles() = runTest {
        val rootDir = Files.createTempDirectory("cookie-file-store").toFile()
        val pathProvider = TempCookieSetPathProvider(rootDir)
        val cookieFileStore = CookieFileStore(pathProvider)

        try {
            val staleFile = pathProvider.tempCookieFile(1L, "stale.txt").apply {
                parentFile?.mkdirs()
                writeText("stale")
                setLastModified(System.currentTimeMillis() - TWO_DAYS_MILLIS)
            }
            val freshFile = pathProvider.tempCookieFile(1L, "fresh.txt").apply {
                parentFile?.mkdirs()
                writeText("fresh")
            }
            val legacyStaleFile = pathProvider.legacyTempFile("legacy-stale.txt").apply {
                parentFile?.mkdirs()
                writeText("legacy stale")
                setLastModified(System.currentTimeMillis() - TWO_DAYS_MILLIS)
            }
            val legacyFreshFile = pathProvider.legacyTempFile("legacy-fresh.txt").apply {
                parentFile?.mkdirs()
                writeText("legacy fresh")
            }

            cookieFileStore.deleteStaleTempCookies()

            assertFalse(staleFile.exists())
            assertTrue(freshFile.exists())
            assertFalse(legacyStaleFile.exists())
            assertTrue(legacyFreshFile.exists())
        } finally {
            rootDir.deleteRecursively()
        }
    }

    private class TempCookieSetPathProvider(
        private val rootDir: File,
    ) : CookieSetPathProvider {
        override fun cookiesDir(): File = File(rootDir, "files/cookies").apply { mkdirs() }

        override fun cookieSetDir(cookieSetId: Long): File {
            return File(cookiesDir(), cookieSetId.toString()).apply { mkdirs() }
        }

        override fun cookieFile(cookieSetId: Long): File {
            return File(cookieSetDir(cookieSetId), "cookies.txt")
        }

        override fun tempDir(): File {
            return File(rootDir, "cache/cookies/temp").apply { mkdirs() }
        }

        override fun tempCookieFile(cookieSetId: Long, fileName: String): File {
            return File(tempDir(), "${cookieSetId}_$fileName")
        }

        fun legacyTempFile(fileName: String): File {
            return File(File(cookiesDir(), "temp"), fileName)
        }
    }

    companion object {
        private const val TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1_000L
    }
}
