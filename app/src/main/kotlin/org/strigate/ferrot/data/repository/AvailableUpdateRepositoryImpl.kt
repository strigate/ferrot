package org.strigate.ferrot.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.mapper.toDomain
import org.strigate.ferrot.data.mapper.toEntity
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvailableUpdateRepositoryImpl @Inject constructor(
    private val availableUpdateDao: AvailableUpdateDao,
) : AvailableUpdateRepository {

    override fun getAsFlow(): Flow<AvailableUpdate?> {
        return availableUpdateDao
            .get()
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun save(update: AvailableUpdate) {
        availableUpdateDao.insertReplace(update.toEntity())
    }

    override suspend fun delete(): Int {
        return availableUpdateDao.delete()
    }
}
