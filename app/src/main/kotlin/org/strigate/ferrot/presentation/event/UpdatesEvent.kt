package org.strigate.ferrot.presentation.event

import androidx.annotation.StringRes

sealed interface UpdatesEvent {
    data class ShowToast(@param:StringRes val textRes: Int) : UpdatesEvent
}
