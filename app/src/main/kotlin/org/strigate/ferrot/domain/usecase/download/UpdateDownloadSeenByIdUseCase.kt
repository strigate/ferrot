package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadSeenByIdUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadId: Long, seen: Boolean = true) {
        downloadRepository.updateSeenById(downloadId, seen)
    }
}
