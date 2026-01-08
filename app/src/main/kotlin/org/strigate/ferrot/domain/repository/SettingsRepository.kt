package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveDownloadWifiOnly(enabled: Boolean)
    fun getDownloadWifiOnlyAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticUpdates(enabled: Boolean)
    fun getAutomaticUpdatesAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDependencyUpdates(enabled: Boolean)
    fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDuplicateDownloadDeletion(enabled: Boolean)
    fun getAutomaticDuplicateDownloadDeletionAsFlow(): Flow<Boolean>
}
