package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getDownloadWifiOnlyAsFlow(): Flow<Boolean>
    suspend fun saveDownloadWifiOnly(enabled: Boolean)
}
