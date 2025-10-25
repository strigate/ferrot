package org.strigate.ferrot.domain.usecase.downloadprogress

import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import javax.inject.Inject

class DeleteDownloadProgressByDownloadIdUseCase @Inject constructor(
    private val downloadProgressRepository: DownloadProgressRepository,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        return downloadProgressRepository.deleteByDownloadId(downloadId) >= 1
    }
}
