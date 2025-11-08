package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadAudioFilePathUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(id: Long, fileName: String?): Boolean {
        return downloadRepository.updateAudioFilePathById(id, fileName) >= 1
    }
}
