package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadErrorMessageUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    suspend operator fun invoke(id: Long, errorMessage: String?) {
        downloadRepository.updateErrorMessageById(id, errorMessage)
    }
}
