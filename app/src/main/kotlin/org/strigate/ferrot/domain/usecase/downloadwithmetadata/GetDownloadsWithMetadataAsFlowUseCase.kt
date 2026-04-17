package org.strigate.ferrot.domain.usecase.downloadwithmetadata

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.repository.DownloadWithMetadataRepository
import javax.inject.Inject

class GetDownloadsWithMetadataAsFlowUseCase @Inject constructor(
    private val downloadWithMetadataRepository: DownloadWithMetadataRepository,
) {
    operator fun invoke(archived: Boolean = false): Flow<List<DownloadWithMetadata>> {
        return if (archived) {
            downloadWithMetadataRepository.getArchivedDownloadsWithMetadataAsFlow()
        } else {
            downloadWithMetadataRepository.getDownloadsWithMetadataAsFlow()
        }
    }
}
