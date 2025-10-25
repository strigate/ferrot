package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.DownloadsUiData

sealed interface DownloadsUiState {
    object Loading : DownloadsUiState
    data class Data(val data: DownloadsUiData) : DownloadsUiState
    object Error : DownloadsUiState
}
