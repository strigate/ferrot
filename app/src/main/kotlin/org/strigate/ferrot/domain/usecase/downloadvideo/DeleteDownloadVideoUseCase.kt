package org.strigate.ferrot.domain.usecase.downloadvideo

import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class DeleteDownloadVideoUseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        return downloadVideoRepository.deleteByDownloadId(downloadId) > 0
    }
}
