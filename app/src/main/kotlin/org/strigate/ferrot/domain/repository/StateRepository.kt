package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow

interface StateRepository {
    fun getBootTimeMillisAsFlow(): Flow<Long>
    suspend fun saveBootTimeMillis(millis: Long)
}
