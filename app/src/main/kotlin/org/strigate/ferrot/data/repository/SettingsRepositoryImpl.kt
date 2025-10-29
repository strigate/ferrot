package org.strigate.ferrot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_DEPENDENCY_UPDATES
import org.strigate.ferrot.app.Constants.Settings.KEY_AUTOMATIC_UPDATES
import org.strigate.ferrot.app.Constants.Settings.KEY_DOWNLOAD_WIFI_ONLY
import org.strigate.ferrot.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) : SettingsRepository {
    private val downloadWifiOnlyKey =
        booleanPreferencesKey(KEY_DOWNLOAD_WIFI_ONLY)
    private val automaticUpdatesKey =
        booleanPreferencesKey(KEY_AUTOMATIC_UPDATES)
    private val automaticDependencyUpdatesKey =
        booleanPreferencesKey(KEY_AUTOMATIC_DEPENDENCY_UPDATES)

    override fun getDownloadWifiOnlyAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[downloadWifiOnlyKey] ?: DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY
        }
    }

    override suspend fun saveDownloadWifiOnly(enabled: Boolean) {
        preferencesDataStore.edit {
            it[downloadWifiOnlyKey] = enabled
        }
    }

    override fun getAutomaticUpdatesAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticUpdatesKey] ?: DEFAULT_VALUE_AUTOMATIC_UPDATES
        }
    }

    override suspend fun saveAutomaticUpdates(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticUpdatesKey] = enabled
        }
    }

    override fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean> {
        return preferencesDataStore.data.map {
            it[automaticDependencyUpdatesKey] ?: DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES
        }
    }

    override suspend fun saveAutomaticDependencyUpdates(enabled: Boolean) {
        preferencesDataStore.edit {
            it[automaticDependencyUpdatesKey] = enabled
        }
    }
}
