package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.app.Constants.Extras.EXTRA_NOTIFICATION_ACTION
import org.strigate.ferrot.app.DownloadNotificationActionType
import org.strigate.ferrot.app.DownloadNotificationActionType.DELETE
import org.strigate.ferrot.app.DownloadNotificationActionType.MARK_SEEN
import org.strigate.ferrot.app.DownloadNotificationActionType.RETRY
import org.strigate.ferrot.app.DownloadNotificationActionType.STOP
import org.strigate.ferrot.app.DownloadNotificationActionType.STOP_ALL
import org.strigate.ferrot.app.DownloadNotificationActionType.UNDO_DELETE
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.activeDownloadNotificationTag
import org.strigate.ferrot.app.buildDownloadNotificationAction
import org.strigate.ferrot.app.downloadNotificationExtras
import org.strigate.ferrot.app.downloadNotificationTag
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import javax.inject.Inject

@AndroidEntryPoint
class DownloadNotificationActionReceiver : BroadcastReceiver() {
    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var downloadUseCase: DownloadUseCase

    @Inject
    lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Inject
    lateinit var downloadProgressUseCase: DownloadProgressUseCase

    @Inject
    lateinit var startDownloadUseCase: StartDownloadUseCase

    @Inject
    lateinit var stopDownloadUseCase: StopDownloadUseCase

    @Inject
    lateinit var clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.getStringExtra(EXTRA_NOTIFICATION_ACTION)
                    ?.let { runCatching { DownloadNotificationActionType.valueOf(it) }.getOrNull() }

                if (action == null) {
                    return@launch
                }
                if (action == STOP_ALL) {
                    handleStopAll()
                    return@launch
                }
                val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID)?.toLongOrNull()
                if (downloadId == null || downloadId <= 0L) {
                    return@launch
                }
                when (action) {
                    MARK_SEEN -> handleMarkSeen(downloadId)
                    DELETE -> handleDelete(downloadId)
                    UNDO_DELETE -> handleUndoDelete(downloadId)
                    RETRY -> handleRetry(downloadId)
                    STOP -> handleStop(downloadId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleMarkSeen(downloadId: Long) {
        downloadUseCase.updateDownloadsSeenUseCase(setOf(downloadId), seen = true)
        clearNotificationsByDownloadIdUseCase(downloadId)
    }

    private suspend fun handleDelete(downloadId: Long) {
        val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return
        downloadUseCase.updateDownloadsPendingDeleteUseCase(setOf(downloadId), pendingDelete = true)
        downloadUseCase.requestDeletePendingDownloadDelayedUseCase(downloadId)
        showDeletePendingNotification(download)
    }

    private suspend fun handleUndoDelete(downloadId: Long) {
        downloadUseCase.updateDownloadsPendingDeleteUseCase(
            setOf(downloadId),
            pendingDelete = false
        )
        showNotificationForDownload(downloadId)
    }

    private suspend fun handleRetry(downloadId: Long) {
        startDownloadUseCase(downloadId)
    }

    private suspend fun handleStop(downloadId: Long) {
        stopActiveDownload(downloadId)
    }

    private suspend fun handleStopAll() {
        downloadUseCase.getAllDownloadsUseCase()
            .asSequence()
            .filter { it.status in ACTIVE_DOWNLOAD_STATUSES }
            .map { it.id }
            .forEach { downloadId -> stopActiveDownload(downloadId) }
    }

    private suspend fun stopActiveDownload(downloadId: Long) {
        runCatching {
            downloadUseCase.updateDownloadStatusUseCase(downloadId, DownloadStatus.STOPPED)
            downloadProgressUseCase.updateDownloadProgressUseCase(
                id = downloadId,
                progressPercent = 0F,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        }
        notificationService.clearNotification(
            notificationId = downloadId.toInt(),
            tag = activeDownloadNotificationTag(downloadId),
        )
        stopDownloadUseCase(downloadId)
    }

    private suspend fun showNotificationForDownload(downloadId: Long) {
        val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return
        when (download.status) {
            DownloadStatus.COMPLETED -> showCompletedNotification(download)
            DownloadStatus.FAILED -> showFailedNotification(download)
            DownloadStatus.METADATA,
            DownloadStatus.DOWNLOADING -> Unit

            else -> clearNotificationsByDownloadIdUseCase(downloadId)
        }
    }

    private suspend fun showCompletedNotification(download: Download) {
        notificationService.notifyDownloaded(
            contentTitle = appContext.getString(R.string.download_complete),
            contentText = notificationText(download),
            extras = downloadNotificationExtras(download.id),
            tag = downloadNotificationTag(download.id),
            actions = buildCompletedActions(download),
        )
    }

    private suspend fun showFailedNotification(download: Download) {
        notificationService.notifyDownloaded(
            contentTitle = appContext.getString(R.string.download_failed),
            contentText = notificationText(download),
            extras = downloadNotificationExtras(download.id),
            tag = downloadNotificationTag(download.id),
            actions = buildFailedActions(download),
        )
    }

    private fun showDeletePendingNotification(download: Download) {
        if (download.status == DownloadStatus.METADATA || download.status == DownloadStatus.DOWNLOADING) {
            return
        }
        val actions = listOf(
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = download.id,
                actionType = UNDO_DELETE,
            ),
        )
        notificationService.notifyDownloaded(
            contentTitle = appContext.getString(R.string.notification_title_deleted),
            contentText = "",
            extras = downloadNotificationExtras(download.id),
            tag = downloadNotificationTag(download.id),
            actions = actions,
        )
    }

    private fun buildCompletedActions(download: Download): List<NotificationCompat.Action> {
        if (download.pendingDelete) {
            return listOf(
                buildDownloadNotificationAction(
                    context = appContext,
                    downloadId = download.id,
                    actionType = UNDO_DELETE,
                ),
            )
        }
        val actions = mutableListOf<NotificationCompat.Action>()
        if (!download.seen) {
            actions += buildDownloadNotificationAction(
                context = appContext,
                downloadId = download.id,
                actionType = MARK_SEEN,
            )
        }
        actions += buildDownloadNotificationAction(
            context = appContext,
            downloadId = download.id,
            actionType = DELETE,
        )
        return actions
    }

    private fun buildFailedActions(download: Download): List<NotificationCompat.Action> {
        if (download.pendingDelete) {
            return listOf(
                buildDownloadNotificationAction(
                    context = appContext,
                    downloadId = download.id,
                    actionType = UNDO_DELETE,
                ),
            )
        }
        return listOf(
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = download.id,
                actionType = RETRY,
            ),
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = download.id,
                actionType = DELETE,
            ),
        )
    }

    private suspend fun notificationText(download: Download): String {
        val metadata = downloadMetadataUseCase
            .getDownloadMetadataByIdAsFlowUseCase(download.id).first()
        return metadata?.title?.takeIf { it.isNotBlank() } ?: download.url
    }

    companion object {
        private val ACTIVE_DOWNLOAD_STATUSES = setOf(
            DownloadStatus.METADATA,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.QUEUED,
            DownloadStatus.WAITING_FOR_NETWORK,
            DownloadStatus.WAITING_FOR_WIFI,
        )
    }
}
