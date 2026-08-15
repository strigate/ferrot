package org.strigate.ferrot.presentation.viewmodel

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.RequestAppUpdateCheckUseCase
import org.strigate.ferrot.domain.usecase.dependencyupdate.RequestDependencyUpdateCheckUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticAppUpdatesEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticAppUpdatesEnabledSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticDependencyUpdatesEnabledSettingUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastAvailableUpdateCheckMillisUseCase
import org.strigate.ferrot.domain.usecase.state.GetLastDependencyUpdateCheckMillisUseCase
import org.strigate.ferrot.presentation.state.UpdatesUiState
import org.strigate.ferrot.test.MainDispatcherRule
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class UpdatesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var stateUseCase: StateUseCase

    @Mock
    private lateinit var saveAutomaticAppUpdatesEnabledSettingUseCase: SaveAutomaticAppUpdatesEnabledSettingUseCase

    @Mock
    private lateinit var saveAutomaticDependencyUpdatesEnabledSettingUseCase: SaveAutomaticDependencyUpdatesEnabledSettingUseCase

    @Mock
    private lateinit var configureAutomaticAppUpdateWorkUseCase: ConfigureAutomaticAppUpdateWorkUseCase

    @Mock
    private lateinit var configureAutomaticDependencyUpdateWorkUseCase: ConfigureAutomaticDependencyUpdateWorkUseCase

    @Mock
    private lateinit var requestAppUpdateCheckUseCase: RequestAppUpdateCheckUseCase

    @Mock
    private lateinit var requestDependencyUpdateCheckUseCase: RequestDependencyUpdateCheckUseCase

    @Mock
    private lateinit var getAutomaticAppUpdatesEnabledSettingAsFlowUseCase: GetAutomaticAppUpdatesEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase: GetAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase

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
        val automaticAppUpdatesEnabledFlow = MutableStateFlow(true)
        val automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false)
        val lastAvailableCheckFlow = MutableStateFlow(123L)
        val lastDependencyCheckFlow = MutableStateFlow(456L)
        val viewModel = createViewModel(
            automaticAppUpdatesEnabledFlow = automaticAppUpdatesEnabledFlow,
            automaticDependencyUpdatesEnabledFlow = automaticDependencyUpdatesEnabledFlow,
            lastAvailableCheckFlow = lastAvailableCheckFlow,
            lastDependencyCheckFlow = lastDependencyCheckFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is UpdatesUiState.Data }

        val state = viewModel.uiState.value as UpdatesUiState.Data
        assertEquals(true, state.data.settings.automaticAppUpdatesEnabled)
        assertEquals(false, state.data.settings.automaticDependencyUpdatesEnabled)
        assertEquals(123L, state.data.info.lastAvailableUpdateCheckMillis)
        assertEquals(456L, state.data.info.lastDependencyUpdateCheckMillis)

        collector.cancel()
    }

    @Test
    fun logShown_logsUpdatesScreen() {
        val viewModel = createViewModel(
            automaticAppUpdatesEnabledFlow = MutableStateFlow(false),
            automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false),
            lastAvailableCheckFlow = MutableStateFlow(0L),
            lastDependencyCheckFlow = MutableStateFlow(0L),
        )

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.UPDATES)
    }

    @Test
    fun setAutomaticAppUpdatesEnabled_savesSetting_andAppliesSchedule() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            automaticAppUpdatesEnabledFlow = MutableStateFlow(false),
            automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false),
            lastAvailableCheckFlow = MutableStateFlow(0L),
            lastDependencyCheckFlow = MutableStateFlow(0L),
        )

        viewModel.setAutomaticAppUpdatesEnabled(true)
        advanceUntilIdle()

        verify(saveAutomaticAppUpdatesEnabledSettingUseCase)
            .invoke(true)
        verify(configureAutomaticAppUpdateWorkUseCase)
            .invoke(true)
    }

    @Test
    fun setAutomaticDependencyUpdatesEnabled_savesSetting_andAppliesSchedule() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                automaticAppUpdatesEnabledFlow = MutableStateFlow(false),
                automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false),
                lastAvailableCheckFlow = MutableStateFlow(0L),
                lastDependencyCheckFlow = MutableStateFlow(0L),
            )

            viewModel.setAutomaticDependencyUpdatesEnabled(true)
            advanceUntilIdle()

            verify(saveAutomaticDependencyUpdatesEnabledSettingUseCase)
                .invoke(true)
            verify(configureAutomaticDependencyUpdateWorkUseCase)
                .invoke(true)
        }

    @Test
    fun checkForAvailableUpdate_requestsCheck() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            automaticAppUpdatesEnabledFlow = MutableStateFlow(false),
            automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false),
            lastAvailableCheckFlow = MutableStateFlow(0L),
            lastDependencyCheckFlow = MutableStateFlow(0L),
        )
        val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.event.collect()
        }

        viewModel.checkForAvailableUpdate()
        advanceUntilIdle()

        verify(requestAppUpdateCheckUseCase)
            .invoke()

        collector.cancel()
    }

    @Test
    fun checkForDependencyUpdates_requestsCheck() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            automaticAppUpdatesEnabledFlow = MutableStateFlow(false),
            automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false),
            lastAvailableCheckFlow = MutableStateFlow(0L),
            lastDependencyCheckFlow = MutableStateFlow(0L),
        )
        val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.event.collect()
        }

        viewModel.checkForDependencyUpdates()
        advanceUntilIdle()

        verify(requestDependencyUpdateCheckUseCase)
            .invoke()

        collector.cancel()
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createViewModel(
        automaticAppUpdatesEnabledFlow: MutableStateFlow<Boolean>,
        automaticDependencyUpdatesEnabledFlow: MutableStateFlow<Boolean>,
        lastAvailableCheckFlow: MutableStateFlow<Long>,
        lastDependencyCheckFlow: MutableStateFlow<Long>,
    ): UpdatesViewModel {
        `when`(getAutomaticAppUpdatesEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticAppUpdatesEnabledFlow)
        `when`(getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDependencyUpdatesEnabledFlow)
        `when`(getLastAvailableUpdateCheckMillisUseCase.invoke())
            .thenReturn(lastAvailableCheckFlow)
        `when`(getLastDependencyUpdateCheckMillisUseCase.invoke())
            .thenReturn(lastDependencyCheckFlow)
        `when`(settingsUseCase.saveAutomaticAppUpdatesEnabledSettingUseCase)
            .thenReturn(saveAutomaticAppUpdatesEnabledSettingUseCase)
        `when`(settingsUseCase.saveAutomaticDependencyUpdatesEnabledSettingUseCase)
            .thenReturn(saveAutomaticDependencyUpdatesEnabledSettingUseCase)
        `when`(settingsUseCase.getAutomaticAppUpdatesEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticAppUpdatesEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase)
        `when`(stateUseCase.getLastAvailableUpdateCheckMillisUseCase)
            .thenReturn(getLastAvailableUpdateCheckMillisUseCase)
        `when`(stateUseCase.getLastDependencyUpdateCheckMillisUseCase)
            .thenReturn(getLastDependencyUpdateCheckMillisUseCase)

        return UpdatesViewModel(
            analyticsLogger = analyticsLogger,
            settingsUseCase = settingsUseCase,
            configureAutomaticAppUpdateWorkUseCase = configureAutomaticAppUpdateWorkUseCase,
            configureAutomaticDependencyUpdateWorkUseCase = configureAutomaticDependencyUpdateWorkUseCase,
            requestAppUpdateCheckUseCase = requestAppUpdateCheckUseCase,
            requestDependencyUpdateCheckUseCase = requestDependencyUpdateCheckUseCase,
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
