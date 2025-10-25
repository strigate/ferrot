package org.strigate.ferrot.app.provider

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.app.Constants.Paths.UPDATES
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface UpdatePathProvider {
    fun updatesDir(): File
    fun apkFileFor(tag: String): File
}

@Singleton
class UpdatePathProviderImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : UpdatePathProvider {
    override fun updatesDir(): File = File(appContext.filesDir, UPDATES).apply { mkdirs() }
    override fun apkFileFor(tag: String): File = File(updatesDir(), "ferrot-$tag.apk")
}
