package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadCompletedAtUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(downloadId: Long, completedAtMillis: Long?) {
        downloadRepository.updateCompletedAtById(downloadId, completedAtMillis)
    }
}
