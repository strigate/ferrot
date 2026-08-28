package org.strigate.ferrot.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_ARCHIVED_DOWNLOADS_GRID_LAYOUT_ENABLED
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_BOOT_TIME_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_DOWNLOADS_GRID_LAYOUT_ENABLED
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_AVAILABLE_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class StateRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    @Test
    fun getters_returnDefaultValues_whenNothingHasBeenSaved() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        assertEquals(
            DEFAULT_VALUE_BOOT_TIME_MILLIS,
            repository.getBootTimeMillisAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_DOWNLOADS_GRID_LAYOUT_ENABLED,
            repository.getDownloadsGridLayoutEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_ARCHIVED_DOWNLOADS_GRID_LAYOUT_ENABLED,
            repository.getArchivedDownloadsGridLayoutEnabledAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_LAST_AVAILABLE_UPDATE_CHECK_MILLIS,
            repository.getLastAvailableUpdateCheckMillisAsFlow().first(),
        )
        assertEquals(
            DEFAULT_VALUE_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS,
            repository.getLastDependencyUpdateCheckMillisAsFlow().first(),
        )
    }

    @Test
    fun saveMethods_persistUpdatedValues() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        repository.saveBootTimeMillis(123L)
        repository.toggleDownloadsGridLayoutEnabled()
        repository.toggleArchivedDownloadsGridLayoutEnabled()
        repository.toggleArchivedDownloadsGridLayoutEnabled()
        repository.saveLastAvailableUpdateCheckMillis(456L)
        repository.saveLastDependencyUpdateCheckMillis(789L)

        assertEquals(123L, repository.getBootTimeMillisAsFlow().first())
        assertEquals(true, repository.getDownloadsGridLayoutEnabledAsFlow().first())
        assertEquals(false, repository.getArchivedDownloadsGridLayoutEnabledAsFlow().first())
        assertEquals(456L, repository.getLastAvailableUpdateCheckMillisAsFlow().first())
        assertEquals(789L, repository.getLastDependencyUpdateCheckMillisAsFlow().first())
    }

    @Test
    fun toggleDownloadsGridLayoutEnabled_appliesConcurrentTogglesAtomically() =
        runTest(testDispatcher) {
            val repository = createRepository(backgroundScope)

            coroutineScope {
                repeat(2) {
                    launch {
                        repository.toggleDownloadsGridLayoutEnabled()
                    }
                }
            }

            assertEquals(
                false,
                repository.getDownloadsGridLayoutEnabledAsFlow().first()
            )
        }

    private fun createRepository(scope: CoroutineScope): StateRepositoryImpl {
        val tempFile = Files.createTempFile("state-repository-test", ".preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFile.toFile() },
        )

        return StateRepositoryImpl(dataStore)
    }
}
