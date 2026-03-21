package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadsSeenUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadIds: Collection<Long>, seen: Boolean = true) {
        if (downloadIds.isEmpty()) {
            return
        }
        downloadRepository.updateSeenByIds(downloadIds, seen)
    }
}
