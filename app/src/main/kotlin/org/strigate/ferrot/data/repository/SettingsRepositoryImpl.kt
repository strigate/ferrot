package org.strigate.ferrot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_APP_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_COOKIES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_WIFI_ONLY_DOWNLOADS_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_APP_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_COOKIES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_WIFI_ONLY_DOWNLOADS_ENABLED
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) : SettingsRepository {
    private val wifiOnlyDownloadsEnabledKey =
        booleanPreferencesKey(KEY_WIFI_ONLY_DOWNLOADS_ENABLED)
    private val automaticDuplicateDownloadDeletionEnabledKey =
        booleanPreferencesKey(KEY_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED)
    private val cookiesEnabledKey =
        booleanPreferencesKey(KEY_COOKIES_ENABLED)
    private val leftSwipeActionKey =
        stringPreferencesKey(KEY_LEFT_SWIPE_ACTION)
    private val rightSwipeActionKey =
        stringPreferencesKey(KEY_RIGHT_SWIPE_ACTION)
    private val automaticAppUpdatesEnabledKey =
        booleanPreferencesKey(KEY_AUTOMATIC_APP_UPDATES_ENABLED)
    private val automaticDependencyUpdatesEnabledKey =
        booleanPreferencesKey(KEY_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED)

    override suspend fun saveWifiOnlyDownloadsEnabled(enabled: Boolean) {
        preferencesDataStore.edit {
            it[wifiOnlyDownloadsEnabledKey] = enabled
        }
    }

    override fun getWifiOnlyDownloadsEnabledAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[wifiOnlyDownloadsEnabledKey] ?: DEFAULT_VALUE_WIFI_ONLY_DOWNLOADS_ENABLED
        }
    }

    override suspend fun saveAutomaticDuplicateDownloadDeletionEnabled(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticDuplicateDownloadDeletionEnabledKey] = enabled
        }
    }

    override fun getAutomaticDuplicateDownloadDeletionEnabledAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticDuplicateDownloadDeletionEnabledKey]
                ?: DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED
        }
    }

    override suspend fun saveCookiesEnabled(enabled: Boolean) {
        preferencesDataStore.edit {
            it[cookiesEnabledKey] = enabled
        }
    }

    override fun getCookiesEnabledAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[cookiesEnabledKey] ?: DEFAULT_VALUE_COOKIES_ENABLED
        }
    }

    override suspend fun saveLeftSwipeAction(action: DownloadSwipeAction) {
        preferencesDataStore.edit {
            it[leftSwipeActionKey] = action.storageValue
        }
    }

    override fun getLeftSwipeActionAsFlow(): Flow<DownloadSwipeAction> {
        return preferencesDataStore.data.map {
            DownloadSwipeAction.fromStorageValue(
                value = it[leftSwipeActionKey],
                defaultAction = DEFAULT_VALUE_LEFT_SWIPE_ACTION,
            )
        }
    }

    override suspend fun saveRightSwipeAction(action: DownloadSwipeAction) {
        preferencesDataStore.edit {
            it[rightSwipeActionKey] = action.storageValue
        }
    }

    override fun getRightSwipeActionAsFlow(): Flow<DownloadSwipeAction> {
        return preferencesDataStore.data.map {
            DownloadSwipeAction.fromStorageValue(
                value = it[rightSwipeActionKey],
                defaultAction = DEFAULT_VALUE_RIGHT_SWIPE_ACTION,
            )
        }
    }

    override suspend fun saveAutomaticAppUpdatesEnabled(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticAppUpdatesEnabledKey] = enabled
        }
    }

    override fun getAutomaticAppUpdatesEnabledAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticAppUpdatesEnabledKey] ?: DEFAULT_VALUE_AUTOMATIC_APP_UPDATES_ENABLED
        }
    }

    override suspend fun saveAutomaticDependencyUpdatesEnabled(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticDependencyUpdatesEnabledKey] = enabled
        }
    }

    override fun getAutomaticDependencyUpdatesEnabledAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticDependencyUpdatesEnabledKey]
                ?: DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED
        }
    }
}
