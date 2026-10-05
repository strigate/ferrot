package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.AdvancedSettingsUiData

sealed interface AdvancedSettingsUiState {
    object Loading : AdvancedSettingsUiState
    data class Data(val data: AdvancedSettingsUiData) : AdvancedSettingsUiState
    object Error : AdvancedSettingsUiState
}
