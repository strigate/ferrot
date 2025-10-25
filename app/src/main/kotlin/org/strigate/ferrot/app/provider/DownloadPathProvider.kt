package org.strigate.ferrot.app.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.app.Constants.Paths.DOWNLOADS
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadPathProvider {
    fun outputDir(): File
    fun uidDir(uid: String): File
}

@Singleton
class DownloadPathProviderImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : DownloadPathProvider {
    override fun outputDir(): File = File(appContext.filesDir, DOWNLOADS).apply { mkdirs() }
    override fun uidDir(uid: String): File = File(outputDir(), uid).apply { mkdirs() }
}
