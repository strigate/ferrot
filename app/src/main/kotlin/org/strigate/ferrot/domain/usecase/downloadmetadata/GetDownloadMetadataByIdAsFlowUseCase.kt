package org.strigate.ferrot.domain.usecase.downloadmetadata

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject

class GetDownloadMetadataByIdAsFlowUseCase @Inject constructor(
    private val downloadRepository: DownloadMetadataRepository,
) {
    operator fun invoke(downloadId: Long): Flow<DownloadMetadata?> {
        return downloadRepository.getByDownloadIdAsFlow(downloadId)
    }
}
