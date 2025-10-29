package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getDownloadWifiOnlyAsFlow(): Flow<Boolean>
    suspend fun saveDownloadWifiOnly(enabled: Boolean)
    fun getAutomaticUpdatesAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticUpdates(enabled: Boolean)
    fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDependencyUpdates(enabled: Boolean)
}
