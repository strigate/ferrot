package org.strigate.ferrot.presentation.model

data class SettingsUiData(
    val downloadWifiOnly: Boolean,
    val automaticDuplicateDownloadDeletion: Boolean,
    val useCookies: Boolean,
    val leftSwipeAction: DownloadSwipeActionUiData,
    val rightSwipeAction: DownloadSwipeActionUiData,
)
