package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadProgressRepositoryImpl @Inject constructor(
    private val downloadProgressDao: DownloadProgressDao,
) : DownloadProgressRepository {
    override suspend fun save(downloadProgress: DownloadProgress): Long {
        return downloadProgressDao.insertReplace(downloadProgress.toEntity())
    }

    override fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadProgress?> {
        return downloadProgressDao
            .getByDownloadIdAsFlow(downloadId)
            .map { it?.toDomain() }
    }

    override suspend fun updateExpectedBytes(downloadId: Long, expectedBytes: Long): Int {
        return downloadProgressDao.updateExpectedBytes(downloadId, expectedBytes)
    }

    override suspend fun deleteByDownloadId(downloadId: Long): Int {
        return downloadProgressDao.deleteByDownloadId(downloadId)
    }
}
