package org.strigate.ferrot.domain.usecase.downloadmetadata

import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject

class SaveDownloadMetadataUseCase @Inject constructor(
    private val downloadMetadataRepository: DownloadMetadataRepository,
) {
    suspend operator fun invoke(meta: DownloadMetadata) {
        downloadMetadataRepository.save(meta)
    }
}
