package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadProgress

interface DownloadProgressRepository {
    suspend fun save(downloadProgress: DownloadProgress): Long
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadProgress?>
    suspend fun updateExpectedBytes(downloadId: Long, expectedBytes: Long): Int
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
