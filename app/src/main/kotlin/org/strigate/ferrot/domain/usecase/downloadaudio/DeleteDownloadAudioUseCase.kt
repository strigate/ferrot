package org.strigate.ferrot.domain.usecase.downloadaudio

import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject

class DeleteDownloadAudioUseCase @Inject constructor(
    private val downloadAudioRepository: DownloadAudioRepository,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        return downloadAudioRepository.deleteByDownloadId(downloadId) > 0
    }
}
