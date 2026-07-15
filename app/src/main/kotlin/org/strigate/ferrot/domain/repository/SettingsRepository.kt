package org.strigate.ferrot.domain.repository

import kotlinx.coroutines.flow.Flow
import org.strigate.ferrot.domain.model.DownloadSwipeAction

interface SettingsRepository {
    suspend fun saveDownloadWifiOnly(enabled: Boolean)
    fun getDownloadWifiOnlyAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDuplicateDownloadDeletion(enabled: Boolean)
    fun getAutomaticDuplicateDownloadDeletionAsFlow(): Flow<Boolean>
    suspend fun saveUseCookies(enabled: Boolean)
    fun getUseCookiesAsFlow(): Flow<Boolean>
    suspend fun saveLeftSwipeAction(action: DownloadSwipeAction)
    fun getLeftSwipeActionAsFlow(): Flow<DownloadSwipeAction>
    suspend fun saveRightSwipeAction(action: DownloadSwipeAction)
    fun getRightSwipeActionAsFlow(): Flow<DownloadSwipeAction>
    suspend fun saveAutomaticUpdates(enabled: Boolean)
    fun getAutomaticUpdatesAsFlow(): Flow<Boolean>
    suspend fun saveAutomaticDependencyUpdates(enabled: Boolean)
    fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean>
}
