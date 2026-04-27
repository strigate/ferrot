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
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_BOOT_TIME_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_AVAILABLE_UPDATE_CHECK_MILLIS
import org.strigate.ferrot.app.Constants.State.DEFAULT_VALUE_LAST_DEPENDENCY_UPDATE_CHECK_MILLIS
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class StateRepositoryImplTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }


    @Test
    fun getters_returnDefaultValues_whenNothingHasBeenSaved() = runTest(testDispatcher) {
        val repository = createRepository(backgroundScope)

        assertEquals(
            DEFAULT_VALUE_BOOT_TIME_MILLIS,
            repository.getBootTimeMillisAsFlow().first(),
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
        repository.saveLastAvailableUpdateCheckMillis(456L)
        repository.saveLastDependencyUpdateCheckMillis(789L)

        assertEquals(123L, repository.getBootTimeMillisAsFlow().first())
        assertEquals(456L, repository.getLastAvailableUpdateCheckMillisAsFlow().first())
        assertEquals(789L, repository.getLastDependencyUpdateCheckMillisAsFlow().first())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
