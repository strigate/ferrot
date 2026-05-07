package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDependencyUpdatesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticUpdatesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastDependencyUpdateCheckMillisUseCase
import org.strigate.ferrot.presentation.state.UpdatesUiState
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatesViewModelTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var stateUseCase: StateUseCase

    @Mock
    private lateinit var getAutomaticUpdatesSettingAsFlowUseCase: GetAutomaticUpdatesSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDependencyUpdatesSettingAsFlowUseCase: GetAutomaticDependencyUpdatesSettingAsFlowUseCase

    @Mock
    private lateinit var getLastAvailableUpdateCheckMillisUseCase: GetLastAvailableUpdateCheckMillisUseCase

    @Mock
    private lateinit var getLastDependencyUpdateCheckMillisUseCase: GetLastDependencyUpdateCheckMillisUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun uiState_exposesMappedSettingsAndInfo() = runTest(testDispatcher) {
        val automaticUpdatesFlow = MutableStateFlow(true)
        val automaticDependencyUpdatesFlow = MutableStateFlow(false)
        val lastAvailableCheckFlow = MutableStateFlow(123L)
        val lastDependencyCheckFlow = MutableStateFlow(456L)
        val viewModel = createViewModel(
            automaticUpdatesFlow = automaticUpdatesFlow,
            automaticDependencyUpdatesFlow = automaticDependencyUpdatesFlow,
            lastAvailableCheckFlow = lastAvailableCheckFlow,
            lastDependencyCheckFlow = lastDependencyCheckFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is UpdatesUiState.Data }

        val state = viewModel.uiState.value as UpdatesUiState.Data
        assertEquals(true, state.data.settings.automaticUpdates)
        assertEquals(false, state.data.settings.automaticDependencyUpdates)
        assertEquals(123L, state.data.info.lastAvailableUpdateCheckMillis)
        assertEquals(456L, state.data.info.lastDependencyUpdateCheckMillis)

        collector.cancel()
    }

    @Test
    fun logShown_logsUpdatesScreen() {
        val viewModel = createViewModel(
            automaticUpdatesFlow = MutableStateFlow(false),
            automaticDependencyUpdatesFlow = MutableStateFlow(false),
            lastAvailableCheckFlow = MutableStateFlow(0L),
            lastDependencyCheckFlow = MutableStateFlow(0L),
        )

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.UPDATES)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createViewModel(
        automaticUpdatesFlow: MutableStateFlow<Boolean>,
        automaticDependencyUpdatesFlow: MutableStateFlow<Boolean>,
        lastAvailableCheckFlow: MutableStateFlow<Long>,
        lastDependencyCheckFlow: MutableStateFlow<Long>,
    ): UpdatesViewModel {
        `when`(getAutomaticUpdatesSettingAsFlowUseCase.invoke())
            .thenReturn(automaticUpdatesFlow)
        `when`(getAutomaticDependencyUpdatesSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDependencyUpdatesFlow)
        `when`(getLastAvailableUpdateCheckMillisUseCase.invoke())
            .thenReturn(lastAvailableCheckFlow)
        `when`(getLastDependencyUpdateCheckMillisUseCase.invoke())
            .thenReturn(lastDependencyCheckFlow)
        `when`(settingsUseCase.getAutomaticUpdatesSettingAsFlowUseCase)
            .thenReturn(getAutomaticUpdatesSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDependencyUpdatesSettingAsFlowUseCase)
            .thenReturn(getAutomaticDependencyUpdatesSettingAsFlowUseCase)
        `when`(stateUseCase.getLastAvailableUpdateCheckMillisUseCase)
            .thenReturn(getLastAvailableUpdateCheckMillisUseCase)
        `when`(stateUseCase.getLastDependencyUpdateCheckMillisUseCase)
            .thenReturn(getLastDependencyUpdateCheckMillisUseCase)

        return UpdatesViewModel(
            appContext = appContext,
            analyticsLogger = analyticsLogger,
            settingsUseCase = settingsUseCase,
            stateUseCase = stateUseCase,
        )
    }

    private suspend fun waitForUiState(
        viewModel: UpdatesViewModel,
        predicate: (UpdatesUiState) -> Boolean,
    ) {
        withTimeout(2.seconds) {
            while (!predicate(viewModel.uiState.value)) {
                kotlinx.coroutines.yield()
            }
        }
    }
}
