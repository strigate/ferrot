package org.strigate.ferrot.domain.usecase.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.app.integration.DownloadWorkScheduler
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.util.NetworkOps
import javax.inject.Inject

class ApplyWifiOnlyPolicyUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val getAllDownloadsUseCase: GetAllDownloadsUseCase,
    private val updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase,
    private val updateDownloadStatusUseCase: UpdateDownloadStatusUseCase,
    private val deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase,
    private val downloadWorkScheduler: DownloadWorkScheduler,
) {
    suspend operator fun invoke(wifiOnlyDownloadsEnabled: Boolean) {
        val (_, isOnWifi) = NetworkOps.quickNetworkProbe(appContext)
        val downloads = getAllDownloadsUseCase()

        if (wifiOnlyDownloadsEnabled) {
            if (!isOnWifi) {
                val downloadStatuses = setOf(
                    DownloadStatus.QUEUED,
                    DownloadStatus.WAITING_FOR_NETWORK,
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
                        downloadWorkScheduler.enqueueOneTimeReplace(download.id, true)
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
                    downloadWorkScheduler.enqueueOneTimeReplace(download.id, false)
                }
        }
    }
}
