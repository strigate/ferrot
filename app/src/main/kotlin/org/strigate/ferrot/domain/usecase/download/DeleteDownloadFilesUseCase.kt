package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.provider.DownloadPathProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteDownloadFilesUseCase @Inject constructor(
    private val downloadPathProvider: DownloadPathProvider,
    private val getDownloadByIdUseCase: GetDownloadByIdUseCase,
) {
    suspend operator fun invoke(downloadId: Long): Boolean = withContext(Dispatchers.IO) {
        val download = getDownloadByIdUseCase(downloadId) ?: return@withContext false
        val uidDir = downloadPathProvider.uidDir(download.uid)
        runCatching { uidDir.deleteRecursively() }.getOrDefault(false)
    }
}
