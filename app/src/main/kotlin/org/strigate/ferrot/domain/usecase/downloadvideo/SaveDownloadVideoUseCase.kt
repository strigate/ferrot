package org.strigate.ferrot.domain.usecase.downloadvideo

import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class SaveDownloadVideoUseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    suspend operator fun invoke(downloadVideo: DownloadVideo): Boolean {
        return downloadVideoRepository.save(downloadVideo) > 0
    }
}
