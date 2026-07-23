package org.strigate.ferrot.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_APP_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_COOKIES_ENABLED
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_WIFI_ONLY_DOWNLOADS_ENABLED
import org.strigate.ferrot.app.Constants.Settings.KEY_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    @Test
    fun getters_returnDefaultValues_whenNothingHasBeenSaved() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        assertEquals(
            DEFAULT_VALUE_WIFI_ONLY_DOWNLOADS_ENABLED,
            repository.getWifiOnlyDownloadsEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION_ENABLED,
            repository.getAutomaticDuplicateDownloadDeletionEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_COOKIES_ENABLED,
            repository.getCookiesEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_LEFT_SWIPE_ACTION,
            repository.getLeftSwipeActionAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_RIGHT_SWIPE_ACTION,
            repository.getRightSwipeActionAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_APP_UPDATES_ENABLED,
            repository.getAutomaticAppUpdatesEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES_ENABLED,
            repository.getAutomaticDependencyUpdatesEnabledAsFlow().first(),
        )
    }

    @Test
    fun saveMethods_persistUpdatedValues() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        repository.saveWifiOnlyDownloadsEnabled(false)
        repository.saveAutomaticDuplicateDownloadDeletionEnabled(false)
        repository.saveCookiesEnabled(true)
        repository.saveLeftSwipeAction(DownloadSwipeAction.NONE)
        repository.saveRightSwipeAction(DownloadSwipeAction.ARCHIVE)
        repository.saveAutomaticAppUpdatesEnabled(false)
        repository.saveAutomaticDependencyUpdatesEnabled(false)

        assertEquals(false, repository.getWifiOnlyDownloadsEnabledAsFlow().first())
        assertEquals(false, repository.getAutomaticDuplicateDownloadDeletionEnabledAsFlow().first())
        assertEquals(true, repository.getCookiesEnabledAsFlow().first())
        assertEquals(DownloadSwipeAction.NONE, repository.getLeftSwipeActionAsFlow().first())
        assertEquals(
            DownloadSwipeAction.ARCHIVE,
            repository.getRightSwipeActionAsFlow().first()
        )
        assertEquals(false, repository.getAutomaticAppUpdatesEnabledAsFlow().first())
        assertEquals(false, repository.getAutomaticDependencyUpdatesEnabledAsFlow().first())
    }

    @Test
    fun swipeGetters_useSideDefaults_forInvalidValue() = runTest(testDispatcher) {
        val tempFile =
            Files.createTempFile("settings-repository-invalid-swipe-test", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempFile.toFile() },
        )
        val repository = SettingsRepositoryImpl(dataStore)

        dataStore.edit {
            it[stringPreferencesKey(KEY_LEFT_SWIPE_ACTION)] = "wat"
            it[stringPreferencesKey(KEY_RIGHT_SWIPE_ACTION)] = "broken"
        }

        assertEquals(
            DEFAULT_VALUE_LEFT_SWIPE_ACTION,
            repository.getLeftSwipeActionAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_RIGHT_SWIPE_ACTION,
            repository.getRightSwipeActionAsFlow().first(),
        )
    }

    private fun createRepository(scope: CoroutineScope): SettingsRepositoryImpl {
        val tempFile = Files.createTempFile("settings-repository-test", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFile.toFile() },
        )

        return SettingsRepositoryImpl(dataStore)
    }
}
