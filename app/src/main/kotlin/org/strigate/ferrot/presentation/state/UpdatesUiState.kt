package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.UpdatesUiData

sealed interface UpdatesUiState {
    object Loading : UpdatesUiState
    data class Data(val data: UpdatesUiData) : UpdatesUiState
    object Error : UpdatesUiState
}
