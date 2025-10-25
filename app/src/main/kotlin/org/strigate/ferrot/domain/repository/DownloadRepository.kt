package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus

interface DownloadRepository {
    suspend fun save(download: Download): Long
    suspend fun getAll(): List<Download>
    suspend fun getById(id: Long): Download?
    fun getByIdAsFlow(id: Long): Flow<Download?>
    suspend fun updateFilePathById(id: Long, filePath: String?): Int
    suspend fun updateStatusById(id: Long, status: DownloadStatus): Int
    suspend fun updateErrorMessageById(id: Long, errorMessage: String?): Int
    suspend fun updateStartedAtById(id: Long, startedAtMillis: Long?): Int
    suspend fun updateCompletedAtById(id: Long, completedAtMillis: Long?): Int
    suspend fun deleteById(id: Long): Int
}
