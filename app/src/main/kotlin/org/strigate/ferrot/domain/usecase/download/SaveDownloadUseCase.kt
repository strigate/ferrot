package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class SaveDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(download: Download): Long {
        return downloadRepository.save(download)
    }
}
