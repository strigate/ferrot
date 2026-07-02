package org.strigate.ferrot.presentation.event

import androidx.annotation.StringRes

sealed interface CookiesEvent {
    data class ShowToast(@param:StringRes val textRes: Int) : CookiesEvent
    data class ShowCookieText(val title: String, val text: String) : CookiesEvent
}
