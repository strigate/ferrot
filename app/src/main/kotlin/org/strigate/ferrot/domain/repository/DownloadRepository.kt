package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus

interface DownloadRepository {
    suspend fun save(download: Download): Long
    suspend fun getAll(): List<Download>
    suspend fun getById(id: Long): Download?
    fun getByIdAsFlow(id: Long): Flow<Download?>
    suspend fun updateStatusById(id: Long, status: DownloadStatus): Int
    suspend fun updateErrorMessageById(id: Long, errorMessage: String?): Int
    suspend fun updateSeenByIds(ids: Collection<Long>, seen: Boolean): Int
    suspend fun updatePendingDeleteByIds(ids: Collection<Long>, pendingDelete: Boolean): Int
    suspend fun updateArchivedByIds(ids: Collection<Long>, archived: Boolean): Int
    suspend fun updateStartedAtById(id: Long, startedAtMillis: Long?): Int
    suspend fun updateCompletedAtById(id: Long, completedAtMillis: Long?): Int
    suspend fun updateCookieSetIdById(id: Long, cookieSetId: Long?): Int
    suspend fun clearCookieSetId(cookieSetId: Long): Int
    suspend fun deleteById(id: Long): Int
}
