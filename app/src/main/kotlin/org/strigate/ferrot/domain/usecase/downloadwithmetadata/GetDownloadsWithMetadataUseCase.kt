package org.strigate.ferrot.domain.usecase.downloadwithmetadata

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.repository.DownloadWithMetadataRepository
import javax.inject.Inject

class GetDownloadsWithMetadataUseCase @Inject constructor(
    private val downloadWithMetadataRepository: DownloadWithMetadataRepository,
) {
    operator fun invoke(): Flow<List<DownloadWithMetadata>> {
        return downloadWithMetadataRepository.getAllDownloadsWithMetadataAsFlow()
    }
}