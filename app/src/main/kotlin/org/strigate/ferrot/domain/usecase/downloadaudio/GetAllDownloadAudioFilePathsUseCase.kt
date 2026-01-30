package org.strigate.ferrot.domain.usecase.downloadaudio

import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject

class GetAllDownloadAudioFilePathsUseCase @Inject constructor(
    private val downloadAudioRepository: DownloadAudioRepository,
) {
    suspend operator fun invoke(): List<String> {
        return downloadAudioRepository.getAllFilePaths()
    }
}
