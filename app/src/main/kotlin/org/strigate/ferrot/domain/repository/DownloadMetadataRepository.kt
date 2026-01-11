package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadMetadata

interface DownloadMetadataRepository {
    suspend fun save(downloadMetadata: DownloadMetadata): Long
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadMetadata?>
    suspend fun getDownloadIdsBySourceAndVideoId(source: String, videoId: String): List<Long>
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
