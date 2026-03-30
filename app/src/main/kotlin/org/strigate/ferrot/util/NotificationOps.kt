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
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.strigate.ferrot.presentation.MainActivity
import kotlin.reflect.KClass

object NotificationOps {
    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }

    private fun findActiveNotification(
        context: Context,
        tag: String?,
        id: Int,
    ): StatusBarNotification? {
        return notificationManager(context)
            .activeNotifications
            .firstOrNull { it.id == id && it.tag == tag }
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
        bigPicture: Bitmap? = null,
        groupId: String? = null,
        tag: String? = null,
        extras: Map<String, String> = emptyMap(),
        actions: List<NotificationCompat.Action> = emptyList(),
        notificationId: Int? = null,
        autoCancel: Boolean = true,
        ongoing: Boolean = false,
        progress: Int? = null,
        indeterminate: Boolean = false,
    ) {
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
            bigPicture = bigPicture,
            priority = priority,
            extras = extras,
            actions = actions,
            notificationId = notificationId,
            autoCancel = autoCancel,
            ongoing = ongoing,
            progress = progress,
            indeterminate = indeterminate,
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
        bigPicture: Bitmap?,
        priority: Int,
        extras: Map<String, String>,
        actions: List<NotificationCompat.Action>,
        notificationId: Int?,
        autoCancel: Boolean,
        ongoing: Boolean,
        progress: Int?,
        indeterminate: Boolean,
        activityClass: KClass<out Activity>,
    ) {
        val id = notificationId ?: tag?.hashCode()
        ?: (System.currentTimeMillis() + System.nanoTime()).toInt()
        val existingNotification = findActiveNotification(
            context = context,
            tag = tag,
            id = id,
        )?.notification
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
            bigPicture = bigPicture,
            priority = priority,
            extras = extras,
            actions = actions,
            autoCancel = autoCancel,
            ongoing = ongoing,
            progress = progress,
            indeterminate = indeterminate,
            whenMillis = existingNotification?.`when`,
            sortKey = existingNotification?.sortKey,
            showWhen = existingNotification?.`when`?.let { it > 0L } ?: true,
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
        bigPicture: Bitmap?,
        priority: Int,
        extras: Map<String, String>,
        actions: List<NotificationCompat.Action>,
        autoCancel: Boolean,
        ongoing: Boolean,
        progress: Int?,
        indeterminate: Boolean,
        whenMillis: Long?,
        sortKey: String?,
        showWhen: Boolean,
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
            .setAutoCancel(autoCancel)
            .setOngoing(ongoing)
            .setGroup(groupId)
            .setShowWhen(showWhen)
            .apply {
                whenMillis?.takeIf { it > 0L }?.let(::setWhen)
                sortKey?.let(::setSortKey)
                if (bigPicture != null) {
                    setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bigPicture)
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
        actions.forEach(notificationBuilder::addAction)
        if (progress != null || indeterminate) {
            notificationBuilder.setProgress(100, progress?.coerceIn(0, 100) ?: 0, indeterminate)
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
        channelId: String? = null,
    ) {
        val notificationManager = notificationManager(context)
        for (statusBarNotification in notificationManager.activeNotifications) {
            if (channelId != null && statusBarNotification.notification.channelId != channelId) {
                continue
            }
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

    fun cancel(
        context: Context,
        notificationId: Int,
        tag: String? = null,
    ) {
        val notificationManager = notificationManager(context)
        if (tag != null) {
            notificationManager.cancel(tag, notificationId)
        } else {
            notificationManager.cancel(notificationId)
        }
    }
}
