package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadAudio

interface DownloadAudioRepository {
    suspend fun save(downloadAudio: DownloadAudio): Long
    fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadAudio?>
    suspend fun deleteByDownloadId(downloadId: Long): Int
}
