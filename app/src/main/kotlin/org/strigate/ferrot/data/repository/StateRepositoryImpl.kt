package org.strigate.ferrot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_BOOT_TIME_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_AVAILABLE_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.app.Constants.State.KEY_BOOT_TIME_MILLIS
import org.strigate.ferrot.app.Constants.State.KEY_LAST_AVAILABLE_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.app.Constants.State.KEY_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.domain.repository.StateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateRepositoryImpl @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) : StateRepository {
    private val bootTimeMillisKey =
        longPreferencesKey(KEY_BOOT_TIME_MILLIS)
    private val lastAvailableUpdateCheckMillisKey =
        longPreferencesKey(KEY_LAST_AVAILABLE_UPDATE_CHECK_MILLIS)
    private val lastDependencyUpdateCheckMillisKey =
        longPreferencesKey(KEY_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS)

    override fun getBootTimeMillisAsFlow(): Flow<Long> {
        return preferencesDataStore.data.map {
            it[bootTimeMillisKey] ?: DEFAULT_VALUE_BOOT_TIME_MILLIS
        }
    }

    override suspend fun saveBootTimeMillis(millis: Long) {
        preferencesDataStore.edit {
            it[bootTimeMillisKey] = millis
        }
    }

    override fun getLastAvailableUpdateCheckMillisAsFlow(): Flow<Long> {
        return preferencesDataStore.data.map {
            it[lastAvailableUpdateCheckMillisKey]
                ?: DEFAULT_VALUE_LAST_AVAILABLE_UPDATE_CHECK_MILLIS
        }
    }

    override suspend fun saveLastAvailableUpdateCheckMillis(millis: Long) {
        preferencesDataStore.edit {
            it[lastAvailableUpdateCheckMillisKey] = millis
        }
    }

    override fun getLastDependencyUpdateCheckMillisAsFlow(): Flow<Long> {
        return preferencesDataStore.data.map {
            it[lastDependencyUpdateCheckMillisKey]
                ?: DEFAULT_VALUE_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS
        }
    }

    override suspend fun saveLastDependencyUpdateCheckMillis(millis: Long) {
        preferencesDataStore.edit {
            it[lastDependencyUpdateCheckMillisKey] = millis
        }
    }
}
