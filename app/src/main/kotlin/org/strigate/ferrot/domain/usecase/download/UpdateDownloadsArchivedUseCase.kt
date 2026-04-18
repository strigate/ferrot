package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class UpdateDownloadsArchivedUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(downloadIds: Collection<Long>, archived: Boolean = true) {
        if (downloadIds.isEmpty()) {
            return
        }
        downloadRepository.updateArchivedByIds(downloadIds, archived)
    }
}
