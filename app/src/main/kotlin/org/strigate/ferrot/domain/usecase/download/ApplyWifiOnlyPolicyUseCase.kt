package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.util.NetworkOps
import org.strigate.ferrot.work.DownloadWorker
import javax.inject.Inject

class ApplyWifiOnlyPolicyUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val downloadUseCase: DownloadUseCase,
) {
    suspend operator fun invoke(isWifiOnly: Boolean) = withContext(Dispatchers.IO) {
        val (_, isOnWifi) = NetworkOps.quickNetworkProbe(appContext)
        val downloads = downloadUseCase.getAllDownloadsUseCase()

        if (isWifiOnly) {
            if (!isOnWifi) {
                val pushToWifiStatuses = setOf(
                    DownloadStatus.QUEUED,
                    DownloadStatus.WAITING_FOR_NETWORK,
                    DownloadStatus.PAUSED,
                    DownloadStatus.METADATA,
                    DownloadStatus.DOWNLOADING,
                )
                downloads.filter { it.status in pushToWifiStatuses }
                    .forEach { download ->
                        runCatching {
                            downloadUseCase.updateDownloadErrorMessageUseCase(
                                download.id,
                                null,
                            )
                        }
                        runCatching {
                            downloadUseCase.updateDownloadStatusByIdUseCase(
                                download.id,
                                DownloadStatus.WAITING_FOR_WIFI,
                            )
                        }
                        runCatching { downloadUseCase.deleteDownloadFilesUseCase(download.id) }
                        DownloadWorker.enqueueOneTimeReplace(
                            context = appContext,
                            id = download.id,
                            wifiOnly = true,
                        )
                    }
            }
        } else {
            downloads.filter { it.status == DownloadStatus.WAITING_FOR_WIFI }
                .forEach { download ->
                    runCatching {
                        downloadUseCase.updateDownloadErrorMessageUseCase(
                            download.id,
                            null,
                        )
                    }
                    runCatching {
                        downloadUseCase.updateDownloadStatusByIdUseCase(
                            download.id,
                            DownloadStatus.WAITING_FOR_NETWORK,
                        )
                    }
                    DownloadWorker.enqueueOneTimeReplace(
                        context = appContext,
                        id = download.id,
                        wifiOnly = false,
                    )
                }
        }
    }
}
