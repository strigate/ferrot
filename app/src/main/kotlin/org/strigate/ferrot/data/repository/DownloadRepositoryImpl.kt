package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.repository.DownloadRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao,
) : DownloadRepository {
    override suspend fun save(download: Download): Long {
        return downloadDao.insert(download.toEntity())
    }

    override suspend fun getAll(): List<Download> {
        return downloadDao
            .getAll()
            .map { it.toDomain() }
    }

    override suspend fun getById(id: Long): Download? {
        return downloadDao.getById(id)?.toDomain()
    }

    override fun getByIdAsFlow(id: Long): Flow<Download?> {
        return downloadDao
            .getByIdAsFlow(id)
            .map { it?.toDomain() }
    }

    override suspend fun updateFilePathById(id: Long, filePath: String?): Int {
        return downloadDao.updateFilePathById(id, filePath)
    }

    override suspend fun updateStatusById(id: Long, status: DownloadStatus): Int {
        return downloadDao.updateStatusById(id, status.toEntity())
    }

    override suspend fun updateErrorMessageById(id: Long, errorMessage: String?): Int {
        return downloadDao.updateErrorMessageById(id, errorMessage)
    }

    override suspend fun updateStartedAtById(id: Long, startedAtMillis: Long?): Int {
        return downloadDao.updateStartedAtById(id, startedAtMillis)
    }

    override suspend fun updateCompletedAtById(id: Long, completedAtMillis: Long?): Int {
        return downloadDao.updateCompletedAtById(id, completedAtMillis)
    }

    override suspend fun deleteById(id: Long): Int {
        return downloadDao.deleteById(id)
    }
}
