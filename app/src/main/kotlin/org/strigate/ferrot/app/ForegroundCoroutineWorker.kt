package org.strigate.ferrot.app

import android.app.NotificationManager
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
import kotlin.math.abs

abstract class ForegroundCoroutineWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    private val defaultForegroundNotificationId: Int = workerParameters.id.hashCode()
        .let {
            if (it == Int.MIN_VALUE) 0 else abs(it)
        }
        .coerceAtLeast(1)

    private var currentNotificationId: Int = defaultForegroundNotificationId
    private var currentExtras: Map<String, String>? = null

    private fun notificationManager(): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }

    suspend fun enableForeground(
        notificationId: Int = defaultForegroundNotificationId,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
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
                actions = actions,
            ),
        )
    }

    protected fun updateForeground(
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
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
                actions = actions,
            ),
        )
    }

    private fun buildForegroundInfo(
        id: Int,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
    ): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java).apply {
            extras?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        val requestCode = if (extras == null) 0 else id
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val existingNotification = notificationManager()
            .activeNotifications
            .firstOrNull { it.id == id }
            ?.notification

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ACTIVE_TASKS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(R.drawable.ic_logo)
            .setChannelId(CHANNEL_ID_ACTIVE_TASKS)
            .setContentTitle(notificationText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(existingNotification?.`when`?.let { it > 0L } ?: true)

        if (contentText != null) {
            builder.setContentText(contentText)
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(contentText),
            )
        }
        existingNotification?.`when`
            ?.takeIf { it > 0L }
            ?.let(builder::setWhen)

        existingNotification?.sortKey?.let(builder::setSortKey)
        extras?.forEach { (key, value) ->
            builder.extras.putString(key, value)
        }
        actions.forEach(builder::addAction)
        if (progress != null || indeterminate) {
            builder.setProgress(100, progress?.coerceIn(0, 100) ?: 0, indeterminate)
        }
        val notification = builder.build()
        return ForegroundInfo(
            id,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
