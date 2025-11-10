package org.strigate.ferrot.domain.usecase.downloadaudio

import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject

class UpdateDownloadAudioFilePathUseCase @Inject constructor(
    private val downloadAudioRepository: DownloadAudioRepository,
) {
    suspend operator fun invoke(downloadId: Long, filePath: String): Boolean {
        val audio = DownloadAudio(
            downloadId = downloadId,
            filePath = filePath,
        )
        return downloadAudioRepository.save(audio) > 0
    }
}
