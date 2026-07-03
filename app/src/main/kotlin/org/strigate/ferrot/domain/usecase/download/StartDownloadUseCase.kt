package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.integration.DownloadWorkScheduler
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.util.NetworkOps
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val settingsUseCase: SettingsUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val downloadWorkScheduler: DownloadWorkScheduler,
) {
    suspend operator fun invoke(downloadId: Long) {
        val wifiOnly = settingsUseCase
            .getDownloadWifiOnlySettingAsFlowUseCase()
            .first()

        val hasInternet = NetworkOps.hasInternetConnection(appContext)
        val isOnWifi = NetworkOps.isOnWifiConnection(appContext)
        val downloadStatus = when {
            !hasInternet -> DownloadStatus.WAITING_FOR_NETWORK
            wifiOnly && !isOnWifi -> DownloadStatus.WAITING_FOR_WIFI
            else -> DownloadStatus.QUEUED
        }
        clearNotificationsByDownloadIdUseCase(downloadId)
        downloadUseCase.updateDownloadStatusUseCase(
            status = downloadStatus,
            downloadId = downloadId,
        )
        Log.d(LOG_TAG, "Enqueuing download: $downloadId ($downloadStatus)")
        downloadWorkScheduler.enqueueOneTimeReplace(downloadId, wifiOnly)
    }
}
