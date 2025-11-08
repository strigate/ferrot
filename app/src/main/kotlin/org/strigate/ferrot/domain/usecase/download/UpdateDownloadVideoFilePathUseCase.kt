package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadVideoFilePathUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(id: Long, fileName: String?): Boolean {
        return downloadRepository.updateVideoFilePathById(id, fileName) >= 1
    }
}
