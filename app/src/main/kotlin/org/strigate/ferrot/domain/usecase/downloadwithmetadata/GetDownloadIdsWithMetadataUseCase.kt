package org.strigate.ferrot.domain.usecase.downloadwithmetadata

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import javax.inject.Inject

class GetDownloadIdsWithMetadataUseCase @Inject constructor(
    private val downloadWithMetadataViewDao: DownloadWithMetadataViewDao,
) {
    operator fun invoke(): Flow<List<Long>> {
        return downloadWithMetadataViewDao.getAllIdsAsFlow()
    }
}
