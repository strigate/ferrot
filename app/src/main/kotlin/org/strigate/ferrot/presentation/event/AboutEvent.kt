package org.strigate.ferrot.presentation.event

sealed interface AboutEvent {
    data object OpenAppInfo : AboutEvent
    data class OpenUrl(val url: String) : AboutEvent
}
