package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow

interface StateRepository {
    fun getBootTimeMillisAsFlow(): Flow<Long>
    suspend fun saveBootTimeMillis(millis: Long)
    fun getDownloadsGridLayoutEnabledAsFlow(): Flow<Boolean>
    suspend fun toggleDownloadsGridLayoutEnabled()
    fun getArchivedDownloadsGridLayoutEnabledAsFlow(): Flow<Boolean>
    suspend fun toggleArchivedDownloadsGridLayoutEnabled()
    fun getLastAvailableUpdateCheckMillisAsFlow(): Flow<Long>
    suspend fun saveLastAvailableUpdateCheckMillis(millis: Long)
    fun getLastDependencyUpdateCheckMillisAsFlow(): Flow<Long>
    suspend fun saveLastDependencyUpdateCheckMillis(millis: Long)
}
