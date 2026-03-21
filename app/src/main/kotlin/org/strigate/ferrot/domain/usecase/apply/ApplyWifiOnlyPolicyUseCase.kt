package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.util.NetworkOps
import org.strigate.ferrot.work.DownloadWorker
import javax.inject.Inject

class ApplyWifiOnlyPolicyUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getAllDownloadsUseCase: GetAllDownloadsUseCase,
    private val updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase,
    private val updateDownloadStatusUseCase: UpdateDownloadStatusUseCase,
    private val deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase,
) {
    suspend operator fun invoke(isWifiOnly: Boolean) = withContext(Dispatchers.IO) {
        val (_, isOnWifi) = NetworkOps.quickNetworkProbe(appContext)
        val downloads = getAllDownloadsUseCase()

        if (isWifiOnly) {
            if (!isOnWifi) {
                val downloadStatuses = setOf(
                    DownloadStatus.QUEUED,
                    DownloadStatus.WAITING_FOR_NETWORK,
                    DownloadStatus.PAUSED,
                    DownloadStatus.METADATA,
                    DownloadStatus.DOWNLOADING,
                )
                downloads
                    .filter {
                        it.status in downloadStatuses
                    }
                    .forEach { download ->
                        runCatching {
                            updateDownloadErrorMessageUseCase(download.id, null)
                        }
                        runCatching {
                            updateDownloadStatusUseCase(
                                status = DownloadStatus.WAITING_FOR_WIFI,
                                downloadId = download.id,
                            )
                        }
                        runCatching {
                            deleteDownloadFilesUseCase(download.id)
                        }
                        DownloadWorker.enqueueOneTimeReplace(
                            context = appContext,
                            id = download.id,
                            wifiOnly = true,
                        )
                    }
            }
        } else {
            downloads
                .filter {
                    it.status == DownloadStatus.WAITING_FOR_WIFI
                }
                .forEach { download ->
                    runCatching {
                        updateDownloadErrorMessageUseCase(download.id, null)
                    }
                    runCatching {
                        updateDownloadStatusUseCase(
                            status = DownloadStatus.WAITING_FOR_NETWORK,
                            downloadId = download.id,
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
