package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.util.NetworkOps
import org.strigate.ferrot.work.DownloadWorker
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val settingsUseCase: SettingsUseCase,
    private val downloadUseCase: DownloadUseCase,
) {
    suspend operator fun invoke(id: Long) = withContext(Dispatchers.IO) {
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
        downloadUseCase.updateDownloadStatusByIdUseCase(
            status = downloadStatus,
            id = id,
        )
        Log.d(LOG_TAG, "Enqueuing download: $id ($downloadStatus)")
        DownloadWorker.enqueueOneTimeReplace(
            context = appContext,
            id = id,
            wifiOnly = wifiOnly,
        )
    }
}
