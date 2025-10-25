package org.strigate.ferrot.domain.usecase.download

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject

class GetAllDownloadsUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository,
) {
    suspend operator fun invoke(): List<Download> {
        return downloadRepository.getAll()
    }
}
