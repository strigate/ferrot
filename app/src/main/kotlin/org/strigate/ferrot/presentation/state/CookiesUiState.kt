package org.strigate.ferrot.presentation.state

import org.strigate.ferrot.presentation.model.CookieSetUiData

sealed interface CookiesUiState {
    object Loading : CookiesUiState
    data class Data(val cookieSets: List<CookieSetUiData>) : CookiesUiState
    object Error : CookiesUiState
}
