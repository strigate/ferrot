package org.strigate.ferrot.util

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.strigate.ferrot.presentation.MainActivity
import kotlin.reflect.KClass

object NotificationOps {
    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }

    fun createNotificationChannelGroup(
        context: Context,
        groupId: String,
        groupName: String,
    ) {
        notificationManager(context).createNotificationChannelGroup(
            NotificationChannelGroup(groupId, groupName),
        )
    }

    fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        channelImportance: Int,
        color: Int,
        groupId: String? = null,
    ) {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            channelImportance,
        ).apply {
            lightColor = context.getColor(color)
            description = channelDescription
            enableLights(true)
            groupId?.let {
                group = it
            }
        }
        notificationManager(context).createNotificationChannel(notificationChannel)
    }

    fun deleteNotificationChannelGroupsOtherThan(
        context: Context,
        channelGroupIds: List<String>,
    ) {
        val notificationManager = notificationManager(context)
        notificationManager.notificationChannelGroups.forEach { notificationChannelGroup ->
            if (!channelGroupIds.contains(notificationChannelGroup.id)) {
                notificationManager.deleteNotificationChannelGroup(notificationChannelGroup.id)
            }
        }
    }

    fun deleteNotificationChannelsOtherThan(
        context: Context,
        channelIds: List<String>,
    ) {
        val notificationManager = notificationManager(context)
        notificationManager.notificationChannels.forEach { notificationChannel ->
            if (!channelIds.contains(notificationChannel.id)) {
                notificationManager.deleteNotificationChannel(notificationChannel.id)
            }
        }
    }

    fun notify(
        context: Context,
        channelId: String,
        priority: Int,
        colorResource: Int,
        iconResource: Int,
        summaryTitleResource: Int,
        contentTitle: String? = null,
        contentText: String? = null,
        largeIcon: Bitmap? = null,
        groupId: String? = null,
        tag: String? = null,
        extras: Map<String, String> = emptyMap(),
    ) = CoroutineScope(Dispatchers.Main).launch {
        notifyInternal(
            context = context,
            channelId = channelId,
            tag = tag,
            groupId = groupId,
            summaryTitleResource = summaryTitleResource,
            contentTitle = contentTitle,
            contentText = contentText,
            colorResource = colorResource,
            iconResource = iconResource,
            largeIcon = largeIcon,
            priority = priority,
            extras = extras,
            activityClass = MainActivity::class,
        )
    }

    private fun notifyInternal(
        context: Context,
        channelId: String,
        tag: String?,
        groupId: String?,
        summaryTitleResource: Int,
        contentTitle: String?,
        contentText: String?,
        colorResource: Int,
        iconResource: Int,
        largeIcon: Bitmap?,
        priority: Int,
        extras: Map<String, String>,
        activityClass: KClass<out Activity>,
    ) {
        val id = tag?.hashCode() ?: (System.currentTimeMillis() + System.nanoTime()).toInt()
        val notification = buildNotification(
            context = context,
            notificationId = id,
            channelId = channelId,
            groupId = groupId,
            contentTitle = contentTitle,
            contentText = contentText,
            colorResource = colorResource,
            iconResource = iconResource,
            largeIcon = largeIcon,
            priority = priority,
            extras = extras,
            activityClass = activityClass,
        )
        val notificationManager = notificationManager(context)
        if (tag != null) {
            notificationManager.notify(tag, id, notification)
        } else {
            notificationManager.notify(id, notification)
        }
        groupId?.let {
            val summaryNotification = buildSummaryNotification(
                context = context,
                channelId = channelId,
                groupId = it,
                titleResource = summaryTitleResource,
                colorResource = colorResource,
                iconResource = iconResource,
                priority = priority,
                activityClass = activityClass,
            )
            notificationManager.notify(it.hashCode(), summaryNotification)
        }
    }

    private fun buildNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        groupId: String?,
        contentTitle: String?,
        contentText: String?,
        colorResource: Int,
        iconResource: Int,
        largeIcon: Bitmap?,
        priority: Int,
        extras: Map<String, String>,
        activityClass: KClass<out Activity>,
    ): Notification {
        val notificationIntent = Intent(context, activityClass.java).apply {
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setColor(Color.valueOf(context.getColor(colorResource)).toArgb())
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(iconResource)
            .setChannelId(channelId)
            .setLargeIcon(largeIcon)
            .setOnlyAlertOnce(true)
            .setPriority(priority)
            .setAutoCancel(true)
            .setGroup(groupId)
            .apply {
                if (largeIcon != null) {
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .setSummaryText(contentText)
                            .bigLargeIcon(largeIcon)
                    )
                } else {
                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(contentText)
                    )
                }
            }

        extras.forEach { (key, value) ->
            notificationBuilder.extras.putString(key, value)
        }
        return notificationBuilder.build()
    }

    private fun buildSummaryNotification(
        context: Context,
        channelId: String,
        groupId: String,
        titleResource: Int,
        colorResource: Int,
        iconResource: Int,
        priority: Int,
        activityClass: KClass<out Activity>,
    ): Notification {
        val notificationIntent = Intent(context, activityClass.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, channelId)
            .setColor(Color.valueOf(context.getColor(colorResource)).toArgb())
            .setContentTitle(context.getString(titleResource))
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setSmallIcon(iconResource)
            .setGroup(groupId)
            .setSilent(true)
            .build()
    }

    fun clearNotificationsByExtraValue(
        context: Context,
        stringExtras: Map<String, String>,
    ) {
        val notificationManager = notificationManager(context)
        for (statusBarNotification in notificationManager.activeNotifications) {
            val extras = statusBarNotification.notification.extras
            var match = true
            for ((key, value) in stringExtras) {
                if (extras.getString(key) != value) {
                    match = false
                    break
                }
            }
            if (match) {
                if (statusBarNotification.tag != null) {
                    notificationManager.cancel(statusBarNotification.tag, statusBarNotification.id)
                } else {
                    notificationManager.cancel(statusBarNotification.id)
                }
            }
        }
    }
}
