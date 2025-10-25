package org.strigate.ferrot.domain.usecase.downloadprogress

import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import javax.inject.Inject

class UpdateDownloadProgressUseCase @Inject constructor(
    private val downloadProgressRepository: DownloadProgressRepository,
) {
    suspend operator fun invoke(
        id: Long,
        progressPercent: Float,
        etaSeconds: Long?,
        bytesDownloaded: Long,
    ): Long = downloadProgressRepository.save(
        DownloadProgress(
            downloadId = id,
            updatedAtMillis = System.currentTimeMillis(),
            progressPercent = progressPercent,
            bytesDownloaded = bytesDownloaded,
            etaSeconds = etaSeconds,
            expectedBytes = null,
        )
    )
}
