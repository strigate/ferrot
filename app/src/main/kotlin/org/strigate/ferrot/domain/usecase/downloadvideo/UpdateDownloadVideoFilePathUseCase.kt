package org.strigate.ferrot.domain.usecase.downloadvideo

import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class UpdateDownloadVideoFilePathUseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    suspend operator fun invoke(downloadId: Long, filePath: String): Boolean {
        val video = DownloadVideo(
            downloadId = downloadId,
            filePath = filePath,
        )
        return downloadVideoRepository.save(video) > 0
    }
}
