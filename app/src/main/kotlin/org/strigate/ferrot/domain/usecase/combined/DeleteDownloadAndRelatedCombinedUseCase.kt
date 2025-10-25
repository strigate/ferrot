package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteDownloadAndRelatedCombinedUseCase @Inject constructor(
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
) {
    suspend operator fun invoke(downloadId: Long): Boolean = withContext(Dispatchers.IO) {
        val downloadFilesDeleted = downloadUseCase
            .deleteDownloadFilesUseCase(downloadId)
        val downloadMetadataDeleted = downloadMetadataUseCase
            .deleteDownloadMetadataByDownloadIdUseCase(downloadId)
        val downloadProgressDeleted = downloadProgressUseCase
            .deleteDownloadProgressByDownloadIdUseCase(downloadId)
        val downloadDeleted = downloadUseCase
            .deleteDownloadByIdUseCase(downloadId)
        clearNotificationsByDownloadIdUseCase(downloadId)
        downloadFilesDeleted
                && downloadMetadataDeleted
                && downloadProgressDeleted
                && downloadDeleted
    }
}
