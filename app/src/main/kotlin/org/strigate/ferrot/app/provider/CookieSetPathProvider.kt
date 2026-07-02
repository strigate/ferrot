package org.strigate.ferrot.app.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.app.Constants.Paths.COOKIES
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface CookieSetPathProvider {
    fun cookiesDir(): File
    fun cookieSetDir(cookieSetId: Long): File
    fun cookieFile(cookieSetId: Long): File
    fun tempDir(): File
    fun tempCookieFile(cookieSetId: Long, fileName: String): File
}

@Singleton
class CookieSetPathProviderImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : CookieSetPathProvider {
    override fun cookiesDir(): File {
        return File(appContext.filesDir, COOKIES).apply { mkdirs() }
    }

    override fun cookieSetDir(cookieSetId: Long): File {
        return File(cookiesDir(), cookieSetId.toString()).apply { mkdirs() }
    }

    override fun cookieFile(cookieSetId: Long): File {
        return File(cookieSetDir(cookieSetId), COOKIE_FILE_NAME)
    }

    override fun tempDir(): File {
        return File(cookiesDir(), TEMP_DIR_NAME).apply { mkdirs() }
    }

    override fun tempCookieFile(cookieSetId: Long, fileName: String): File {
        return File(tempDir(), "${cookieSetId}_$fileName")
    }

    companion object {
        private const val COOKIE_FILE_NAME = "cookies.txt"
        private const val TEMP_DIR_NAME = "temp"
    }
}
