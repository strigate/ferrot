package org.strigate.ferrot.domain.usecase.downloadprogress

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import javax.inject.Inject

class GetDownloadProgressByDownloadIdAsFlowUseCase @Inject constructor(
    private val downloadProgressRepository: DownloadProgressRepository,
) {
    operator fun invoke(downloadId: Long): Flow<DownloadProgress?> {
        return downloadProgressRepository.getByDownloadIdAsFlow(downloadId)
    }
}
