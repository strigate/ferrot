package org.strigate.ferrot.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
    }

    @Test
    fun saveMethods_persistUpdatedValues() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        repository.saveDownloadWifiOnly(false)
        repository.saveAutomaticUpdates(false)
        repository.saveAutomaticDependencyUpdates(false)
        repository.saveAutomaticDuplicateDownloadDeletion(false)

        assertEquals(false, repository.getDownloadWifiOnlyAsFlow().first())
        assertEquals(false, repository.getAutomaticUpdatesAsFlow().first())
        assertEquals(false, repository.getAutomaticDependencyUpdatesAsFlow().first())
        assertEquals(false, repository.getAutomaticDuplicateDownloadDeletionAsFlow().first())
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
