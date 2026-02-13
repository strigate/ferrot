package org.strigate.ferrot.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Notifications.Channels.CHANNEL_ID_ACTIVE_TASKS
import org.strigate.ferrot.presentation.MainActivity

abstract class ForegroundCoroutineWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    private var currentNotificationId: Long = 1L
    private var currentExtras: Map<String, String>? = null

    suspend fun enableForeground(
        notificationId: Long = 1,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        currentNotificationId = notificationId
        currentExtras = extras
        setForeground(
            buildForegroundInfo(
                id = notificationId,
                notificationText = notificationText,
                progress = progress,
                indeterminate = indeterminate,
                contentText = contentText,
                extras = extras,
            ),
        )
    }

    protected fun updateForeground(
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        if (extras != null) {
            currentExtras = extras
        }
        setForegroundAsync(
            buildForegroundInfo(
                id = currentNotificationId,
                notificationText = notificationText,
                progress = progress,
                indeterminate = indeterminate,
                contentText = contentText,
                extras = currentExtras,
            ),
        )
    }

    private fun buildForegroundInfo(
        id: Long,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            extras?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        val requestCode = if (extras == null) 0 else id.toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ACTIVE_TASKS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(R.drawable.ic_logo)
            .setChannelId(CHANNEL_ID_ACTIVE_TASKS)
            .setContentTitle(notificationText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (contentText != null) {
            builder.setContentText(contentText)
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(contentText),
            )
        }
        if (progress != null || indeterminate) {
            builder.setProgress(100, progress?.coerceIn(0, 100) ?: 0, indeterminate)
        }
        val notification = builder.build()
        return ForegroundInfo(
            id.toInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
