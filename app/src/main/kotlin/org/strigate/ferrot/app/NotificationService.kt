package org.strigate.ferrot.app

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Notifications.ChannelGroups.ALL_CHANNEL_GROUP_IDS
import org.strigate.ferrot.app.Constants.Notifications.ChannelGroups.CHANNEL_GROUP_ID_FOREGROUND
import org.strigate.ferrot.app.Constants.Notifications.ChannelGroups.CHANNEL_GROUP_ID_GENERAL
import org.strigate.ferrot.app.Constants.Notifications.Channels.ALL_CHANNEL_IDS
import org.strigate.ferrot.app.Constants.Notifications.Channels.CHANNEL_ID_ACTIVE_DOWNLOADS
import org.strigate.ferrot.app.Constants.Notifications.Channels.CHANNEL_ID_DOWNLOADED
import org.strigate.ferrot.app.Constants.Notifications.Groups.GROUP_ID_DOWNLOADED
import org.strigate.ferrot.util.NotificationOps.createNotificationChannel
import org.strigate.ferrot.util.NotificationOps.createNotificationChannelGroup
import org.strigate.ferrot.util.NotificationOps.deleteNotificationChannelGroupsOtherThan
import org.strigate.ferrot.util.NotificationOps.deleteNotificationChannelsOtherThan
import org.strigate.ferrot.util.NotificationOps.notify
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    fun initializeNotificationChannels() {
        deleteNotificationChannelGroupsOtherThan(
            context = appContext,
            channelGroupIds = ALL_CHANNEL_GROUP_IDS,
        )
        deleteNotificationChannelsOtherThan(
            context = appContext,
            channelIds = ALL_CHANNEL_IDS,
        )
        createNotificationChannelGroup(
            context = appContext,
            groupId = CHANNEL_GROUP_ID_FOREGROUND,
            groupName = appContext.getString(R.string.notification_channel_group_name_foreground),
        )
        createNotificationChannelGroup(
            context = appContext,
            groupId = CHANNEL_GROUP_ID_GENERAL,
            groupName = appContext.getString(R.string.notification_channel_group_name_general),
        )
        createNotificationChannel(
            context = appContext,
            channelId = CHANNEL_ID_ACTIVE_DOWNLOADS,
            channelName = appContext.getString(R.string.notification_channel_name_active_downloads),
            channelDescription = appContext.getString(R.string.notification_channel_description_active_downloads),
            channelImportance = NotificationManager.IMPORTANCE_LOW,
            color = R.color.coral,
            groupId = CHANNEL_GROUP_ID_FOREGROUND,
        )
        createNotificationChannel(
            context = appContext,
            channelId = CHANNEL_ID_DOWNLOADED,
            channelName = appContext.getString(R.string.notification_channel_name_downloaded),
            channelDescription = appContext.getString(R.string.notification_channel_description_downloaded),
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
            color = R.color.coral,
            groupId = CHANNEL_GROUP_ID_GENERAL,
        )
    }

    fun notifyDownloaded(
        contentTitle: String,
        contentText: String,
        extras: Map<String, String> = emptyMap(),
        tag: String? = null,
    ) {
        notify(
            context = appContext,
            channelId = CHANNEL_ID_DOWNLOADED,
            groupId = GROUP_ID_DOWNLOADED,
            summaryTitleResource = R.string.notification_summary_title,
            contentTitle = contentTitle,
            contentText = contentText,
            colorResource = R.color.coral,
            iconResource = R.drawable.ic_logo,
            largeIcon = null,
            priority = NotificationCompat.PRIORITY_HIGH,
            tag = tag,
            extras = extras,
        )
    }
}
