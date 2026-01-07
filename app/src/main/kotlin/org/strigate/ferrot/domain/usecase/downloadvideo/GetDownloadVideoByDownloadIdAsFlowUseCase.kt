package org.strigate.ferrot.domain.usecase.downloadvideo

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject

class GetDownloadVideoByDownloadIdAsFlowUseCase @Inject constructor(
    private val downloadVideoRepository: DownloadVideoRepository,
) {
    operator fun invoke(downloadId: Long): Flow<DownloadVideo?> {
        return downloadVideoRepository.getByDownloadIdAsFlow(downloadId)
    }
}
