package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadAudioDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadAudioRepositoryImpl @Inject constructor(
    private val downloadAudioDao: DownloadAudioDao,
) : DownloadAudioRepository {
    override suspend fun save(downloadAudio: DownloadAudio): Long {
        return downloadAudioDao.insertReplace(downloadAudio.toEntity())
    }

    override fun getByDownloadIdAsFlow(downloadId: Long): Flow<DownloadAudio?> {
        return downloadAudioDao
            .getByDownloadIdAsFlow(downloadId)
            .map { it?.toDomain() }
    }

    override suspend fun getAllFilePaths(): List<String> {
        return downloadAudioDao.getAllFilePaths()
    }

    override suspend fun deleteByDownloadId(downloadId: Long): Int {
        return downloadAudioDao.deleteByDownloadId(downloadId)
    }
}
