package org.strigate.ferrot.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_AUTOMATIC_UPDATES
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.DEFAULT_VALUE_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_LEFT_SWIPE_ACTION
import org.strigate.ferrot.app.Constants.Settings.KEY_RIGHT_SWIPE_ACTION
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }


    @Test
    fun getters_returnDefaultValues_whenNothingHasBeenSaved() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        assertEquals(
            DEFAULT_VALUE_DOWNLOAD_WIFI_ONLY,
            repository.getDownloadWifiOnlyAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_UPDATES,
            repository.getAutomaticUpdatesAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_DEPENDENCY_UPDATES,
            repository.getAutomaticDependencyUpdatesAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_AUTOMATIC_DUPLICATE_DOWNLOAD_DELETION,
            repository.getAutomaticDuplicateDownloadDeletionAsFlow().first(),
        )
        assertEquals(
            DownloadSwipeAction.fromStorageValue(DEFAULT_VALUE_LEFT_SWIPE_ACTION),
            repository.getLeftSwipeActionAsFlow().first(),
        )
        assertEquals(
            DownloadSwipeAction.fromStorageValue(DEFAULT_VALUE_RIGHT_SWIPE_ACTION),
            repository.getRightSwipeActionAsFlow().first(),
        )
    }

    @Test
    fun saveMethods_persistUpdatedValues() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        repository.saveDownloadWifiOnly(false)
        repository.saveAutomaticUpdates(false)
        repository.saveAutomaticDependencyUpdates(false)
        repository.saveAutomaticDuplicateDownloadDeletion(false)
        repository.saveLeftSwipeAction(DownloadSwipeAction.NONE)
        repository.saveRightSwipeAction(DownloadSwipeAction.ARCHIVE)

        assertEquals(false, repository.getDownloadWifiOnlyAsFlow().first())
        assertEquals(false, repository.getAutomaticUpdatesAsFlow().first())
        assertEquals(false, repository.getAutomaticDependencyUpdatesAsFlow().first())
        assertEquals(false, repository.getAutomaticDuplicateDownloadDeletionAsFlow().first())
        assertEquals(DownloadSwipeAction.NONE, repository.getLeftSwipeActionAsFlow().first())
        assertEquals(
            DownloadSwipeAction.ARCHIVE,
            repository.getRightSwipeActionAsFlow().first()
        )
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

        assertEquals(DownloadSwipeAction.ARCHIVE, repository.getLeftSwipeActionAsFlow().first())
        assertEquals(DownloadSwipeAction.DELETE, repository.getRightSwipeActionAsFlow().first())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
