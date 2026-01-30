package org.strigate.ferrot.domain.usecase.downloadmetadata

import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject

class GetAllDownloadThumbnailFilePathsUseCase @Inject constructor(
    private val downloadMetadataRepository: DownloadMetadataRepository,
) {
    suspend operator fun invoke(): List<String> {
        return downloadMetadataRepository.getAllThumbnailFilePaths()
    }
}
