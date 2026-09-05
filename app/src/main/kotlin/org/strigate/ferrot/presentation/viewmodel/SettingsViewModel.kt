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
import org.strigate.ferrot.domain.usecase.apply.ApplyWifiOnlyPolicyUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase
import org.strigate.ferrot.presentation.mapper.toDomain
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.model.SettingsUiData
import org.strigate.ferrot.presentation.state.SettingsUiState
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val settingsUseCase: SettingsUseCase,
    private val applyWifiOnlyPolicyUseCase: ApplyWifiOnlyPolicyUseCase,
    private val configureAutomaticDuplicateDownloadDeletionWorkUseCase: ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState.Loading,
    )

    private fun getUiState(): Flow<SettingsUiState> {
        return combine(
            settingsUseCase.getWifiOnlyDownloadsEnabledSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase(),
            settingsUseCase.getCookiesEnabledSettingAsFlowUseCase(),
            settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase(),
            settingsUseCase.getRightSwipeActionSettingAsFlowUseCase(),
        ) { wifiOnlyDownloadsEnabled,
            automaticDuplicateDownloadDeletionEnabled,
            cookiesEnabled,
            leftSwipeAction,
            rightSwipeAction ->
            val uiState: SettingsUiState = SettingsUiState.Data(
                SettingsUiData(
                    wifiOnlyDownloadsEnabled = wifiOnlyDownloadsEnabled,
                    automaticDuplicateDownloadDeletionEnabled = automaticDuplicateDownloadDeletionEnabled,
                    cookiesEnabled = cookiesEnabled,
                    leftSwipeAction = leftSwipeAction.toUiData(),
                    rightSwipeAction = rightSwipeAction.toUiData(),
                )
            )
            uiState
        }.catch { emit(SettingsUiState.Error) }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.SETTINGS)

    fun setWifiOnlyDownloadsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveWifiOnlyDownloadsEnabledSettingUseCase(enabled)
            applyWifiOnlyPolicyUseCase(enabled)
        }
    }

    fun setAutomaticDuplicateDownloadDeletionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase(enabled)
            configureAutomaticDuplicateDownloadDeletionWorkUseCase(enabled)
        }
    }

    fun setCookiesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveCookiesEnabledSettingUseCase(enabled)
        }
    }

    fun setLeftSwipeAction(action: DownloadSwipeActionUiData) {
        viewModelScope.launch {
            settingsUseCase.saveLeftSwipeActionSettingUseCase(action.toDomain())
        }
    }

    fun setRightSwipeAction(action: DownloadSwipeActionUiData) {
        viewModelScope.launch {
            settingsUseCase.saveRightSwipeActionSettingUseCase(action.toDomain())
        }
    }

    companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
