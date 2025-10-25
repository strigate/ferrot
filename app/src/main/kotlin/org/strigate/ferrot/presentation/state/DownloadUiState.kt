package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.DownloadUiData

sealed interface DownloadUiState {
    data object Loading : DownloadUiState
    data class Data(val data: DownloadUiData) : DownloadUiState
    data object Error : DownloadUiState
}
