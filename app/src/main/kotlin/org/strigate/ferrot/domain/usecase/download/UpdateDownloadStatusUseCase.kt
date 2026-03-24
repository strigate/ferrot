package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadStatusUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadId: Long, status: DownloadStatus): Boolean {
        return downloadRepository.updateStatusById(downloadId, status) >= 1
    }
}
