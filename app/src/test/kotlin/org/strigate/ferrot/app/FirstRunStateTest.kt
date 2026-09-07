package org.strigate.ferrot.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.strigate.ferrot.test.MainDispatcherRule
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class FirstRunStateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    @Test
    fun isFirstRun_returnsTrueUntilMarkedInitialized() = runTest(testDispatcher) {
        val state = createState(backgroundScope)

        assertTrue(state.isFirstRun())
        state.markInitialized()
        assertFalse(state.isFirstRun())
    }

    @Test
    fun markInitialized_isIdempotent() = runTest(testDispatcher) {
        val state = createState(backgroundScope)

        assertTrue(state.isFirstRun())
        state.markInitialized()
        assertFalse(state.isFirstRun())
        state.markInitialized()
        assertFalse(state.isFirstRun())
    }

    private fun createState(scope: CoroutineScope): FirstRunState {
        val tempFile = Files.createTempFile("first-run-state-test", ".preferences_pb").apply {
            toFile().deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFile.toFile() },
        )
        return FirstRunState(dataStore)
    }
}
