package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.AvailableUpdate

interface AvailableUpdateRepository {
    fun getAsFlow(): Flow<AvailableUpdate?>
    suspend fun save(update: AvailableUpdate)
    suspend fun delete(): Int
}
