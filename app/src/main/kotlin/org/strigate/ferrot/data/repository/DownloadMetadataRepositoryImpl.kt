package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadMetadataRepositoryImpl @Inject constructor(
    private val downloadMetadataDao: DownloadMetadataDao,
) : DownloadMetadataRepository {
    override suspend fun save(downloadMetadata: DownloadMetadata): Long {
        return downloadMetadataDao.insertReplace(downloadMetadata.toEntity())
    }

    override fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadMetadata?> {
        return downloadMetadataDao
            .getByDownloadIdAsFlow(downloadId)
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun deleteByDownloadId(downloadId: Long): Int {
        return downloadMetadataDao.deleteByDownloadId(downloadId)
    }
}
