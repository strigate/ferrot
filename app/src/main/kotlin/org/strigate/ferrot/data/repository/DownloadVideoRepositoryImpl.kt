package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadVideoDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadVideoRepositoryImpl @Inject constructor(
    private val downloadVideoDao: DownloadVideoDao,
) : DownloadVideoRepository {
    override suspend fun save(downloadVideo: DownloadVideo): Long {
        return downloadVideoDao.insertOrReplace(downloadVideo.toEntity())
    }

    override fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadVideo?> {
        return downloadVideoDao.getByDownloadIdAsFlow(downloadId).map { it?.toDomain() }
    }

    override suspend fun deleteByDownloadId(downloadId: Long): Int {
        return downloadVideoDao.deleteByDownloadId(downloadId)
    }
}
