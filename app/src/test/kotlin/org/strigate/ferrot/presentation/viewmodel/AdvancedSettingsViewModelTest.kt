package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.settings.GetIncludeAttributionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetStripMediaMetadataEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveIncludeAttributionEnabledSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveStripMediaMetadataEnabledSettingUseCase
import org.strigate.ferrot.presentation.state.AdvancedSettingsUiState
import org.strigate.ferrot.test.MainDispatcherRule
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancedSettingsViewModelTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val includeAttributionEnabledFlow = MutableStateFlow(true)

    private val stripMediaMetadataEnabledFlow = MutableStateFlow(false)

    private val viewModels = mutableListOf<AdvancedSettingsViewModel>()

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var getIncludeAttributionEnabledSettingAsFlowUseCase: GetIncludeAttributionEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getStripMediaMetadataEnabledSettingAsFlowUseCase: GetStripMediaMetadataEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var saveIncludeAttributionEnabledSettingUseCase: SaveIncludeAttributionEnabledSettingUseCase

    @Mock
    private lateinit var saveStripMediaMetadataEnabledSettingUseCase: SaveStripMediaMetadataEnabledSettingUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(settingsUseCase.getIncludeAttributionEnabledSettingAsFlowUseCase)
            .thenReturn(getIncludeAttributionEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getStripMediaMetadataEnabledSettingAsFlowUseCase)
            .thenReturn(getStripMediaMetadataEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.saveIncludeAttributionEnabledSettingUseCase)
            .thenReturn(saveIncludeAttributionEnabledSettingUseCase)
        `when`(settingsUseCase.saveStripMediaMetadataEnabledSettingUseCase)
            .thenReturn(saveStripMediaMetadataEnabledSettingUseCase)
        `when`(getIncludeAttributionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(includeAttributionEnabledFlow)
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(stripMediaMetadataEnabledFlow)
    }

    @Test
    fun statePreservesAttributionWhileStrippingIsEnabled() = runTest {
        val viewModel = createViewModel()
        val initial = viewModel.uiState.first {
            it is AdvancedSettingsUiState.Data
        } as AdvancedSettingsUiState.Data

        assertEquals(true, initial.data.includeAttributionEnabled)
        assertEquals(false, initial.data.stripMediaMetadataEnabled)

        stripMediaMetadataEnabledFlow.value = true
        val enabled = viewModel.uiState.first {
            it is AdvancedSettingsUiState.Data && it.data.stripMediaMetadataEnabled
        } as AdvancedSettingsUiState.Data
        assertEquals(true, enabled.data.includeAttributionEnabled)

        stripMediaMetadataEnabledFlow.value = false
        val restored = viewModel.uiState.first {
            it is AdvancedSettingsUiState.Data && !it.data.stripMediaMetadataEnabled
        } as AdvancedSettingsUiState.Data
        assertEquals(true, restored.data.includeAttributionEnabled)

        includeAttributionEnabledFlow.value = false
        val changed = viewModel.uiState.first {
            it is AdvancedSettingsUiState.Data && !it.data.includeAttributionEnabled
        } as AdvancedSettingsUiState.Data
        assertEquals(false, changed.data.includeAttributionEnabled)
    }

    @Test
    fun savesStrippingWithoutOverwritingAttribution() = runTest {
        createViewModel().setStripMediaMetadataEnabled(true)
        advanceUntilIdle()
        verify(saveStripMediaMetadataEnabledSettingUseCase)
            .invoke(true)
        verifyNoInteractions(saveIncludeAttributionEnabledSettingUseCase)
    }

    @Test
    fun savesAttribution() = runTest {
        createViewModel().setIncludeAttributionEnabled(false)
        advanceUntilIdle()
        verify(saveIncludeAttributionEnabledSettingUseCase)
            .invoke(false)
    }

    @Test
    fun exposesReadFailure() = runTest {
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(flow { throw IOException("unavailable") })
        val state = createViewModel().uiState.first { it is AdvancedSettingsUiState.Error }
        assertEquals(AdvancedSettingsUiState.Error, state)
    }

    @Test
    fun logsAdvancedSettingsScreen() {
        createViewModel().logShown()
        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.ADVANCED)
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        autoCloseable.close()
    }

    private fun createViewModel() = AdvancedSettingsViewModel(
        analyticsLogger = analyticsLogger,
        settingsUseCase = settingsUseCase,
    ).also { viewModels += it }
}
