package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadWithMetadata

interface DownloadWithMetadataRepository {
    fun getDownloadsWithMetadataAsFlow(): Flow<List<DownloadWithMetadata>>
    fun getArchivedDownloadsWithMetadataAsFlow(): Flow<List<DownloadWithMetadata>>
}
