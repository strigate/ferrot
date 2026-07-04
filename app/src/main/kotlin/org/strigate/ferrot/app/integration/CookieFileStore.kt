package org.strigate.ferrot.app.integration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.provider.CookieSetPathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieFileStore @Inject constructor(
    private val cookieSetPathProvider: CookieSetPathProvider,
) {
    suspend fun writeCookies(cookieSetId: Long, content: String): File {
        return withContext(Dispatchers.IO) {
            cookieSetPathProvider.cookieFile(cookieSetId).apply {
                parentFile?.mkdirs()
                writeText(content)
            }
        }
    }

    suspend fun copyCookies(cookieSetId: Long, source: File): File {
        return withContext(Dispatchers.IO) {
            cookieSetPathProvider.cookieFile(cookieSetId).apply {
                parentFile?.mkdirs()
                source.copyTo(this, overwrite = true)
            }
        }
    }

    suspend fun copyCookiesToTemp(cookieSetId: Long, name: String): File? {
        return withContext(Dispatchers.IO) {
            val source = cookieSetPathProvider.cookieFile(cookieSetId)
            if (!source.exists() || source.length() <= 0L) {
                return@withContext null
            }
            cookieSetPathProvider.tempCookieFile(cookieSetId, name).apply {
                parentFile?.mkdirs()
                source.copyTo(this, overwrite = true)
            }
        }
    }

    suspend fun readCookies(cookieSetId: Long): String? {
        return withContext(Dispatchers.IO) {
            val source = cookieSetPathProvider.cookieFile(cookieSetId)
            if (!source.exists() || source.length() <= 0L) {
                return@withContext null
            }
            source.readText()
        }
    }

    suspend fun deleteCookieSet(cookieSetId: Long) {
        withContext(Dispatchers.IO) {
            cookieSetPathProvider.cookieSetDir(cookieSetId).deleteRecursively()
        }
    }

    suspend fun delete(file: File?) {
        withContext(Dispatchers.IO) {
            file?.delete()
        }
    }

    suspend fun deleteStaleTempCookies() {
        withContext(Dispatchers.IO) {
            val cutoffMillis = System.currentTimeMillis() - STALE_TEMP_COOKIE_MAX_AGE_MILLIS
            listOf(
                cookieSetPathProvider.tempDir(),
                legacyTempDir(),
            ).distinctBy { directory -> directory.absolutePath }
                .forEach { directory -> deleteStaleTempCookies(directory, cutoffMillis) }
        }
    }

    private fun deleteStaleTempCookies(directory: File, cutoffMillis: Long) {
        directory
            .listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.lastModified() < cutoffMillis }
            .forEach { file -> file.delete() }
    }

    private fun legacyTempDir(): File {
        return File(cookieSetPathProvider.cookiesDir(), LEGACY_TEMP_DIR_NAME)
    }

    companion object {
        private const val LEGACY_TEMP_DIR_NAME = "temp"
        private const val STALE_TEMP_COOKIE_MAX_AGE_MILLIS = 24 * 60 * 60 * 1_000L
    }
}
