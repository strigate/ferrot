package org.strigate.ferrot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_USE_COOKIES
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_DEPENDENCY_UPDATES
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_UPDATES
import org.strigate.ferrot.app.Constants.Settings.KEY_DOWNLOAD_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Settings.KEY_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_USE_COOKIES
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) : SettingsRepository {
    private val downloadWifiOnlyKey =
        booleanPreferencesKey(KEY_DOWNLOAD_WIFI_ONLY)
    private val automaticDuplicateDownloadDeletionKey =
        booleanPreferencesKey(KEY_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION)
    private val useCookiesKey =
        booleanPreferencesKey(KEY_USE_COOKIES)
    private val leftSwipeActionKey =
        stringPreferencesKey(KEY_LEFT_SWIPE_ACTION)
    private val rightSwipeActionKey =
        stringPreferencesKey(KEY_RIGHT_SWIPE_ACTION)
    private val automaticUpdatesKey =
        booleanPreferencesKey(KEY_AUTOMATIC_UPDATES)
    private val automaticDependencyUpdatesKey =
        booleanPreferencesKey(KEY_AUTOMATIC_DEPENDENCY_UPDATES)

    override suspend fun saveDownloadWifiOnly(enabled: Boolean) {
        preferencesDataStore.edit {
            it[downloadWifiOnlyKey] = enabled
        }
    }

    override fun getDownloadWifiOnlyAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[downloadWifiOnlyKey] ?: DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY
        }
    }

    override suspend fun saveAutomaticDuplicateDownloadDeletion(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticDuplicateDownloadDeletionKey] = enabled
        }
    }

    override fun getAutomaticDuplicateDownloadDeletionAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticDuplicateDownloadDeletionKey]
                ?: DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION
        }
    }

    override suspend fun saveUseCookies(enabled: Boolean) {
        preferencesDataStore.edit {
            it[useCookiesKey] = enabled
        }
    }

    override fun getUseCookiesAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[useCookiesKey] ?: DEFAULT_VALUE_USE_COOKIES
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

    override suspend fun saveAutomaticUpdates(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticUpdatesKey] = enabled
        }
    }

    override fun getAutomaticUpdatesAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticUpdatesKey] ?: DEFAULT_VALUE_AUTOMATIC_UPDATES
        }
    }

    override suspend fun saveAutomaticDependencyUpdates(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticDependencyUpdatesKey] = enabled
        }
    }

    override fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticDependencyUpdatesKey] ?: DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES
        }
    }
}
