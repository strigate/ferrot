package org.strigate.ferrot.domain.usecase.downloadmetadata

import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject

class DeleteDownloadMetadataByDownloadIdUseCase @Inject constructor(
    private val downloadMetadataRepository: DownloadMetadataRepository,
) {
    suspend operator fun invoke(downloadId: Long): Boolean {
        return downloadMetadataRepository.deleteByDownloadId(downloadId) >= 1
    }
}
