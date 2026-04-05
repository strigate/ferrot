package org.strigate.ferrot.presentation.event

sealed interface DownloadsEvent {
    data class InstallUpdate(val path: String) : DownloadsEvent
}
