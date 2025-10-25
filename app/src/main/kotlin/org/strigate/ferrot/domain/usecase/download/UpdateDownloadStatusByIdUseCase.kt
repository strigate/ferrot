package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadStatusByIdUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(id: Long, status: DownloadStatus): Boolean {
        return downloadRepository.updateStatusById(id, status) >= 1
    }
}
