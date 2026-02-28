package org.strigate.ferrot.presentation.event

sealed interface AboutNavigationEvent {
    data object OpenAppInfo : AboutNavigationEvent
    data class OpenUrl(val url: String) : AboutNavigationEvent
}
