package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class MarkDownloadsPendingDeleteUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(
        downloadIds: Collection<Long>,
        pendingDelete: Boolean = true,
    ) {
        if (downloadIds.isEmpty()) {
            return
        }
        downloadRepository.updatePendingDeleteByIds(downloadIds, pendingDelete)
    }
}
