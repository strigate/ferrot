package org.strigate.ferrot.presentation.event

sealed interface DownloadEvent {
    data object NavigateBack : DownloadEvent
    data class Play(val path: String) : DownloadEvent
    data class Share(val path: String) : DownloadEvent
    data class Save(val path: String) : DownloadEvent
}
