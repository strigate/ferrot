package org.strigate.ferrot.domain.usecase.downloadmetadata

import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject

class GetDownloadIdsBySourceAndVideoIdUseCase @Inject constructor(
    private val downloadMetadataRepository: DownloadMetadataRepository,
) {
    suspend operator fun invoke(source: String, videoId: String): List<Long> {
        return downloadMetadataRepository.getDownloadIdsBySourceAndVideoId(
            source = source,
            videoId = videoId,
        )
    }
}
