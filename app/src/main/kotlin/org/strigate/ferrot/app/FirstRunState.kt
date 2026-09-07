package org.strigate.ferrot.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.app.Constants.State.KEY_FIRST_RUN_MILLIS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirstRunState @Inject constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) {
    private val firstRunMillisKey = longPreferencesKey(KEY_FIRST_RUN_MILLIS)

    suspend fun isFirstRun(): Boolean {
        return preferencesDataStore.data.first()[firstRunMillisKey] == null
    }

    suspend fun markInitialized() {
        preferencesDataStore.edit { preferences ->
            if (preferences[firstRunMillisKey] == null) {
                preferences[firstRunMillisKey] = System.currentTimeMillis()
            }
        }
    }
}
