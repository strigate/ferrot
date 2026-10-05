package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.presentation.model.AdvancedSettingsUiData
import org.strigate.ferrot.presentation.state.AdvancedSettingsUiState
import javax.inject.Inject

@HiltViewModel
class AdvancedSettingsViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {
    val uiState: StateFlow<AdvancedSettingsUiState> = getUiState()
        .catch { emit(AdvancedSettingsUiState.Error) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AdvancedSettingsUiState.Loading,
        )

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.ADVANCED)

    private fun getUiState(): Flow<AdvancedSettingsUiState> {
        return combine(
            flow = settingsUseCase.getStripMediaMetadataEnabledSettingAsFlowUseCase(),
            flow2 = settingsUseCase.getIncludeAttributionEnabledSettingAsFlowUseCase(),
        ) { strip, attribution ->
            val uiState: AdvancedSettingsUiState = AdvancedSettingsUiState.Data(
                data = AdvancedSettingsUiData(
                    stripMediaMetadataEnabled = strip,
                    includeAttributionEnabled = attribution,
                ),
            )
            uiState
        }
    }

    fun setStripMediaMetadataEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveStripMediaMetadataEnabledSettingUseCase(enabled = enabled)
        }
    }

    fun setIncludeAttributionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveIncludeAttributionEnabledSettingUseCase(enabled = enabled)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
