package org.strigate.ferrot.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Action.ACTION_AVAILABLE_UPDATE_NOTIFICATION
import org.strigate.ferrot.app.Constants.Action.ACTION_INSTALL_AVAILABLE_UPDATE
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOADS
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_AVAILABLE_UPDATE_APK_FILE_PATH
import org.strigate.ferrot.app.Constants.Extras.EXTRA_AVAILABLE_UPDATE_VERSION_TAG
import org.strigate.ferrot.app.Constants.Extras.EXTRA_NOTIFICATION_ACTION
import org.strigate.ferrot.app.receiver.AvailableUpdateNotificationActionReceiver
import org.strigate.ferrot.presentation.MainActivity

enum class AvailableUpdateNotificationActionType {
    DELETE,
}

fun availableUpdateNotificationTag(): String = "update_available"

fun availableUpdateNotificationExtras(): Map<String, String> = mapOf(
    EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOADS,
)

fun buildInstallAvailableUpdateNotificationAction(
    context: Context,
    apkFilePath: String,
    versionTag: String,
): NotificationCompat.Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_ACTION, ACTION_INSTALL_AVAILABLE_UPDATE)
        putExtra(EXTRA_AVAILABLE_UPDATE_APK_FILE_PATH, apkFilePath)
        putExtra(EXTRA_AVAILABLE_UPDATE_VERSION_TAG, versionTag)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        (apkFilePath + versionTag + ACTION_INSTALL_AVAILABLE_UPDATE).hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Action.Builder(
        R.drawable.ic_logo,
        context.getString(R.string.notification_action_install),
        pendingIntent,
    ).build()
}

fun buildDeleteAvailableUpdateNotificationAction(
    context: Context,
): NotificationCompat.Action {
    val intent = Intent(context, AvailableUpdateNotificationActionReceiver::class.java).apply {
        action = ACTION_AVAILABLE_UPDATE_NOTIFICATION
        putExtra(EXTRA_NOTIFICATION_ACTION, AvailableUpdateNotificationActionType.DELETE.name)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        AvailableUpdateNotificationActionType.DELETE.name.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return NotificationCompat.Action.Builder(
        R.drawable.ic_logo,
        context.getString(R.string.notification_action_delete),
        pendingIntent,
    ).build()
}
