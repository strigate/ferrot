package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ApplyWifiOnlyPolicyUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase
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
    private val configureAutomaticDuplicateDownloadDeletionSettingUseCase: ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState.Loading,
    )

    private fun getUiState(): Flow<SettingsUiState> {
        return combine(
            settingsUseCase.getDownloadWifiOnlySettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase(),
            settingsUseCase.getUseCookiesSettingAsFlowUseCase(),
            settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase(),
            settingsUseCase.getRightSwipeActionSettingAsFlowUseCase(),
        ) { downloadWifiOnly,
            automaticDuplicateDownloadDeletion,
            useCookies,
            leftSwipeAction,
            rightSwipeAction ->
            SettingsUiState.Data(
                SettingsUiData(
                    downloadWifiOnly = downloadWifiOnly,
                    automaticDuplicateDownloadDeletion = automaticDuplicateDownloadDeletion,
                    useCookies = useCookies,
                    leftSwipeAction = leftSwipeAction.toUiData(),
                    rightSwipeAction = rightSwipeAction.toUiData(),
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

    fun setAutomaticDuplicateDownloadDeletion(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticDuplicateDownloadDeletionSettingUseCase(enabled)
            configureAutomaticDuplicateDownloadDeletionSettingUseCase(enabled)
        }
    }

    fun setUseCookies(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveUseCookiesSettingUseCase(enabled)
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
