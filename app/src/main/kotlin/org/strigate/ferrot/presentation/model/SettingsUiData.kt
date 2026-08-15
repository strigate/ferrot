package org.strigate.ferrot.presentation.model

data class SettingsUiData(
    val wifiOnlyDownloadsEnabled: Boolean,
    val automaticDuplicateDownloadDeletionEnabled: Boolean,
    val cookiesEnabled: Boolean,
    val leftSwipeAction: DownloadSwipeActionUiData,
    val rightSwipeAction: DownloadSwipeActionUiData,
)
