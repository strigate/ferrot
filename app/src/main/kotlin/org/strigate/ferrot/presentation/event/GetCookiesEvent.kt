package org.strigate.ferrot.presentation.event

import androidx.annotation.StringRes

sealed interface GetCookiesEvent {
    data class ShowToast(@param:StringRes val textRes: Int) : GetCookiesEvent
    data class ShowOverwriteConfirmation(
        val url: String,
        val domain: String,
    ) : GetCookiesEvent

    object Saved : GetCookiesEvent
}
