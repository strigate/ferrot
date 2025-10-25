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
import org.strigate.ferrot.app.Constants.Notifications.Channels.CHANNEL_ID_ACTIVE_DOWNLOADS
import org.strigate.ferrot.presentation.MainActivity

abstract class ForegroundCoroutineWorker(
    private val context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {
    suspend fun enableForeground(
        notificationId: Long = 1,
        notificationText: String,
    ) {
        setForeground(createForegroundInfo(notificationId, notificationText))
    }

    private fun createForegroundInfo(id: Long, notificationText: String): ForegroundInfo {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ACTIVE_DOWNLOADS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(R.drawable.ic_logo)
            .setChannelId(CHANNEL_ID_ACTIVE_DOWNLOADS)
            .setContentTitle(notificationText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        return ForegroundInfo(
            id.toInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
