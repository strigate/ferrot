package org.strigate.ferrot.presentation.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.presentation.event.AboutEvent

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun logShown_logsAboutScreen() {
        val viewModel = AboutViewModel(analyticsLogger)

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.ABOUT)
    }

    @Test
    fun onUrlClicked_emitsOpenUrlEvent() = runTest(testDispatcher) {
        val viewModel = AboutViewModel(analyticsLogger)
        val event = async { viewModel.event.first() }

        viewModel.onUrlClicked("https://example.com")
        advanceUntilIdle()

        assertEquals(AboutEvent.OpenUrl("https://example.com"), event.await())
    }

    @Test
    fun onBuildClicked_emitsOpenAppInfoEvent() = runTest(testDispatcher) {
        val viewModel = AboutViewModel(analyticsLogger)
        val event = async { viewModel.event.first() }

        viewModel.onBuildClicked()
        advanceUntilIdle()

        assertEquals(AboutEvent.OpenAppInfo, event.await())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }
}
