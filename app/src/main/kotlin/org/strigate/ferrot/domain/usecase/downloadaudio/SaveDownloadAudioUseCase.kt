package org.strigate.ferrot.domain.usecase.downloadaudio

import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject

class SaveDownloadAudioUseCase @Inject constructor(
    private val downloadAudioRepository: DownloadAudioRepository,
) {
    suspend operator fun invoke(downloadAudio: DownloadAudio): Boolean {
        return downloadAudioRepository.save(downloadAudio) > 0
    }
}
