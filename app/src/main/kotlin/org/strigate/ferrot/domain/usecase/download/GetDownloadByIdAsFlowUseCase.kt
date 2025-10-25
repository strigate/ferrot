package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class GetDownloadByIdAsFlowUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    operator fun invoke(downloadId: Long): Flow<Download?> {
        return downloadRepository.getByIdAsFlow(downloadId)
    }
}
