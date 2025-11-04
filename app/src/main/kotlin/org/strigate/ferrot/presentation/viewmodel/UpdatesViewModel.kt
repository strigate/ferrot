package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.model.UpdatesUiData
import org.strigate.ferrot.presentation.state.UpdatesUiState
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val analyticsLogger: AnalyticsLogger,
    private val settingsUseCase: SettingsUseCase,
    private val stateUseCase: StateUseCase,
) : ViewModel() {
    val uiState: StateFlow<UpdatesUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UpdatesUiState.Loading,
    )

    private fun getUiState(): Flow<UpdatesUiState> {
        return combine(
            settingsUseCase.getAutomaticUpdatesSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDependencyUpdatesSettingAsFlowUseCase(),
            stateUseCase.getLastAvailableUpdateCheckMillisUseCase(),
            stateUseCase.getLastDependencyUpdateCheckMillisUseCase(),
        ) { automaticUpdates, automaticDependencyUpdates, lastAppCheckMillis, lastDepsCheckMillis ->
            UpdatesUiState.Data(
                UpdatesUiData(
                    automaticUpdates = automaticUpdates,
                    automaticDependencyUpdates = automaticDependencyUpdates,
                    lastAvailableUpdateCheckMillis = lastAppCheckMillis,
                    lastDependencyUpdateCheckMillis = lastDepsCheckMillis,
                ),
            )
        }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.UPDATES)

    fun setAutomaticUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticUpdatesSettingUseCase(enabled)
            if (enabled) {
                DownloadAvailableUpdateWorker.enqueuePeriodicKeep(appContext)
            } else {
                DownloadAvailableUpdateWorker.cancelPeriodic(appContext)
            }
        }
    }

    fun checkForAvailableUpdate() {
        viewModelScope.launch {
            appContext.toast(appContext.getString(R.string.toast_checking_for_available_update))
            DownloadAvailableUpdateWorker.enqueueOneTimeReplace(appContext)
        }
    }

    fun setAutomaticDependencyUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticDependencyUpdatesSettingAsFlowUseCase(enabled)
            if (enabled) {
                UpdateDependenciesWorker.enqueuePeriodicKeep(appContext)
            } else {
                UpdateDependenciesWorker.cancelPeriodic(appContext)
            }
        }
    }

    companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
