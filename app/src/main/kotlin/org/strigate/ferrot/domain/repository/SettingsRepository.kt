package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadSwipeAction

interface SettingsRepository {
    suspend fun saveWifiOnlyDownloadsEnabled(enabled: Boolean)
    fun getWifiOnlyDownloadsEnabledAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDuplicateDownloadDeletionEnabled(enabled: Boolean)
    fun getAutomaticDuplicateDownloadDeletionEnabledAsFlow(): Flow<Boolean>
    suspend fun saveCookiesEnabled(enabled: Boolean)
    fun getCookiesEnabledAsFlow(): Flow<Boolean>
    suspend fun saveLeftSwipeAction(action: DownloadSwipeAction)
    fun getLeftSwipeActionAsFlow(): Flow<DownloadSwipeAction>
    suspend fun saveRightSwipeAction(action: DownloadSwipeAction)
    fun getRightSwipeActionAsFlow(): Flow<DownloadSwipeAction>
    suspend fun saveAutomaticAppUpdatesEnabled(enabled: Boolean)
    fun getAutomaticAppUpdatesEnabledAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDependencyUpdatesEnabled(enabled: Boolean)
    fun getAutomaticDependencyUpdatesEnabledAsFlow(): Flow<Boolean>
}
