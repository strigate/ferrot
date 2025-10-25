package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.repository.DownloadWithMetadataRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadWithMetadataRepositoryImpl @Inject constructor(
    private val downloadWithMetadataViewDao: DownloadWithMetadataViewDao,
) : DownloadWithMetadataRepository {
    override fun getAllDownloadsWithMetadataAsFlow(): Flow<List<DownloadWithMetadata>> {
        return downloadWithMetadataViewDao
            .getAllAsFlow()
            .map { views ->
                views.map { it.toDomain() }
            }
    }
}
