package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.download.ApplyWifiOnlyPolicyUseCase
import org.strigate.ferrot.presentation.model.SettingsUiData
import org.strigate.ferrot.presentation.state.SettingsUiState
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val settingsUseCase: SettingsUseCase,
    private val applyWifiOnlyPolicyUseCase: ApplyWifiOnlyPolicyUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState.Loading,
    )

    private fun getUiState(): Flow<SettingsUiState> {
        return settingsUseCase
            .getDownloadWifiOnlySettingAsFlowUseCase()
            .map { downloadWifiOnly ->
                SettingsUiState.Data(
                    SettingsUiData(
                        downloadWifiOnly = downloadWifiOnly,
                    )
                )
            }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.SETTINGS)

    fun setDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveDownloadWifiOnlySettingUseCase(enabled)
            applyWifiOnlyPolicyUseCase(enabled)
        }
    }

    companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
