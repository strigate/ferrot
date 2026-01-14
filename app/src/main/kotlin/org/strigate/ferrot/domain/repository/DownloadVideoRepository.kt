package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadVideo

interface DownloadVideoRepository {
    suspend fun save(downloadVideo: DownloadVideo): Long
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadVideo?>
    suspend fun getDownloadIdsBySha256(sha256: String): List<Long>
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
