package org.strigate.ferrot.domain.usecase.downloadaudio

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject

class GetDownloadAudioAsFlowUseCase @Inject constructor(
    private val downloadAudioRepository: DownloadAudioRepository,
) {
    operator fun invoke(downloadId: Long): Flow<DownloadAudio?> {
        return downloadAudioRepository.getByDownloadIdAsFlow(downloadId)
    }
}
