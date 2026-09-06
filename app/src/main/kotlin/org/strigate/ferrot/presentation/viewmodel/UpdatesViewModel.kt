package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.RequestAppUpdateCheckUseCase
import org.strigate.ferrot.domain.usecase.dependencyupdate.RequestDependencyUpdateCheckUseCase
import org.strigate.ferrot.presentation.event.UpdatesEvent
import org.strigate.ferrot.presentation.model.UpdatesInfoUiData
import org.strigate.ferrot.presentation.model.UpdatesSettingsUiData
import org.strigate.ferrot.presentation.model.UpdatesUiData
import org.strigate.ferrot.presentation.state.UpdatesUiState
import javax.inject.Inject

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val settingsUseCase: SettingsUseCase,
    private val configureAutomaticAppUpdateWorkUseCase: ConfigureAutomaticAppUpdateWorkUseCase,
    private val requestAppUpdateCheckUseCase: RequestAppUpdateCheckUseCase,
    private val configureAutomaticDependencyUpdateWorkUseCase: ConfigureAutomaticDependencyUpdateWorkUseCase,
    private val requestDependencyUpdateCheckUseCase: RequestDependencyUpdateCheckUseCase,
    private val stateUseCase: StateUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<UpdatesEvent>()
    val event = _event.asSharedFlow()

    val uiState: StateFlow<UpdatesUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UpdatesUiState.Loading,
    )

    private fun getUiState(): Flow<UpdatesUiState> {
        return combine(
            settingsUseCase.getAutomaticAppUpdatesEnabledSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase(),
            stateUseCase.getLastAvailableUpdateCheckMillisUseCase(),
            stateUseCase.getLastDependencyUpdateCheckMillisUseCase(),
        ) { automaticAppUpdatesEnabled,
            automaticDependencyUpdatesEnabled,
            lastAvailableUpdateCheckMillis,
            lastDependencyUpdateCheckMillis ->
            val uiState: UpdatesUiState = UpdatesUiState.Data(
                UpdatesUiData(
                    settings = UpdatesSettingsUiData(
                        automaticAppUpdatesEnabled = automaticAppUpdatesEnabled,
                        automaticDependencyUpdatesEnabled = automaticDependencyUpdatesEnabled,
                    ),
                    info = UpdatesInfoUiData(
                        lastAvailableUpdateCheckMillis = lastAvailableUpdateCheckMillis,
                        lastDependencyUpdateCheckMillis = lastDependencyUpdateCheckMillis,
                    ),
                ),
            )
            uiState
        }.catch { emit(UpdatesUiState.Error) }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.UPDATES)

    fun setAutomaticAppUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticAppUpdatesEnabledSettingUseCase(enabled)
            configureAutomaticAppUpdateWorkUseCase(enabled)
        }
    }

    fun checkForAvailableUpdate() {
        viewModelScope.launch {
            _event.emit(UpdatesEvent.ShowToast(R.string.toast_checking_for_app_updates))
            requestAppUpdateCheckUseCase()
        }
    }

    fun setAutomaticDependencyUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCase.saveAutomaticDependencyUpdatesEnabledSettingUseCase(enabled)
            configureAutomaticDependencyUpdateWorkUseCase(enabled)
        }
    }

    fun checkForDependencyUpdates() {
        viewModelScope.launch {
            _event.emit(UpdatesEvent.ShowToast(R.string.toast_checking_for_dependency_updates))
            requestDependencyUpdateCheckUseCase()
        }
    }

    companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
