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
}
