package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteDownloadByIdUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        return downloadRepository.deleteById(downloadId) >= 1
    }
}
