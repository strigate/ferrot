package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.presentation.event.AboutEvent
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    private val viewModels = mutableListOf<AboutViewModel>()

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun logShown_logsAboutScreen() {
        val viewModel = createViewModel()

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.ABOUT)
    }

    @Test
    fun onUrlClicked_emitsOpenUrlEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val event = async { viewModel.event.first() }

        viewModel.onUrlClicked("https://example.com")
        advanceUntilIdle()

        assertEquals(AboutEvent.OpenUrl("https://example.com"), event.await())
    }

    @Test
    fun onBuildClicked_emitsOpenAppInfoEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val event = async { viewModel.event.first() }

        viewModel.onBuildClicked()
        advanceUntilIdle()

        assertEquals(AboutEvent.OpenAppInfo, event.await())
    }

    @After
    fun tearDown() = runTest(testDispatcher) {
        viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        autoCloseable.close()
    }

    private fun createViewModel() = AboutViewModel(analyticsLogger).also { viewModels += it }
}
