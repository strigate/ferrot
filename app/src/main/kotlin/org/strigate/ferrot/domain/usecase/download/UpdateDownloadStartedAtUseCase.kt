package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadStartedAtUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(id: Long, startedAtMillis: Long?) {
        downloadRepository.updateStartedAtById(id, startedAtMillis)
    }
}
