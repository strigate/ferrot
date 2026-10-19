package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteDownloadAndRelatedCombinedUseCase @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val downloadUseCase: DownloadUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
) {
    suspend operator fun invoke(downloadId: Long): Boolean = withContext(ioDispatcher) {
        clearNotificationsByDownloadIdUseCase(downloadId)
        val downloadFilesDeleted = downloadUseCase.deleteDownloadFilesUseCase(downloadId)
        downloadMetadataUseCase.deleteDownloadMetadataByDownloadIdUseCase(downloadId)
        downloadProgressUseCase.deleteDownloadProgressByDownloadIdUseCase(downloadId)
        downloadAudioUseCase.deleteDownloadAudioUseCase(downloadId)
        downloadVideoUseCase.deleteDownloadVideoUseCase(downloadId)
        val downloadDeleted = downloadUseCase.deleteDownloadByIdUseCase(downloadId)
        downloadFilesDeleted && downloadDeleted
    }
}
