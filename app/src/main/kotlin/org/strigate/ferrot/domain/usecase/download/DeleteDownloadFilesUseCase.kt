package org.strigate.ferrot.domain.usecase.download

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.app.provider.DownloadPathProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteDownloadFilesUseCase @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val downloadPathProvider: DownloadPathProvider,
    private val getDownloadByIdUseCase: GetDownloadByIdUseCase,
) {
    suspend operator fun invoke(downloadId: Long): Boolean = withContext(ioDispatcher) {
        val download = getDownloadByIdUseCase(downloadId) ?: return@withContext false
        val uidDir = downloadPathProvider.uidDir(download.uid)
        if (!uidDir.exists()) {
            return@withContext true
        }
        runCatching {
            uidDir.deleteRecursively()
        }.onFailure {
            Log.w(LOG_TAG, "Failed to delete download files for uid=${download.uid}", it)
        }.getOrDefault(false)
    }
}
