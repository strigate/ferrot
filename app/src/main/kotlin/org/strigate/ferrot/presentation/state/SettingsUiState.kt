package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.SettingsUiData

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Data(val data: SettingsUiData) : SettingsUiState
    object Error : SettingsUiState
}
