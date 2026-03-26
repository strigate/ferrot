package org.strigate.ferrot.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Action.ACTION_DOWNLOAD_NOTIFICATION
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.app.Constants.Extras.EXTRA_NOTIFICATION_ACTION
import org.strigate.ferrot.app.receiver.DownloadNotificationActionReceiver
import java.util.UUID

enum class DownloadNotificationActionType {
    MARK_SEEN,
    DELETE,
    UNDO_DELETE,
    RETRY,
    STOP,
}

fun downloadNotificationTag(downloadId: Long): String = "download:$downloadId"

fun activeDownloadNotificationTag(downloadId: Long): String = "active-download:$downloadId"

fun downloadNotificationExtras(downloadId: Long): Map<String, String> = mapOf(
    EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOAD,
    EXTRA_DOWNLOAD_ID to downloadId.toString(),
)

fun buildDownloadNotificationAction(
    context: Context,
    downloadId: Long,
    actionType: DownloadNotificationActionType,
): NotificationCompat.Action {
    val titleResource = when (actionType) {
        DownloadNotificationActionType.MARK_SEEN -> R.string.notification_action_mark_seen
        DownloadNotificationActionType.DELETE -> R.string.notification_action_delete
        DownloadNotificationActionType.UNDO_DELETE -> R.string.notification_action_undo
        DownloadNotificationActionType.RETRY -> R.string.notification_action_retry
        DownloadNotificationActionType.STOP -> R.string.notification_action_stop
    }
    val intent = Intent(context, DownloadNotificationActionReceiver::class.java).apply {
        action = ACTION_DOWNLOAD_NOTIFICATION
        putExtra(EXTRA_DOWNLOAD_ID, downloadId.toString())
        putExtra(EXTRA_NOTIFICATION_ACTION, actionType.name)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        (downloadId.toString() + actionType.name).hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Action.Builder(
        R.drawable.ic_logo,
        context.getString(titleResource),
        pendingIntent,
    ).build()
}

fun buildCancelDownloadNotificationAction(
    context: Context,
    workId: UUID,
): NotificationCompat.Action {
    return NotificationCompat.Action.Builder(
        R.drawable.ic_logo,
        context.getString(R.string.notification_action_stop),
        WorkManager.getInstance(context).createCancelPendingIntent(workId),
    ).build()
}
