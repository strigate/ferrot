package org.strigate.ferrot.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.util.calculateDailyTriggerAtMillis

internal object DailyWorkAlarmScheduler {
    fun schedule(
        context: Context,
        receiverClass: Class<out BroadcastReceiver>,
        requestCode: Int,
        targetHour: Int,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = createPendingIntent(
            context = context,
            receiverClass = receiverClass,
            requestCode = requestCode,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        val triggerAtMillis = calculateDailyTriggerAtMillis(targetHour)
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
            Log.d(LOG_TAG, "Scheduled idle-allowed daily alarm for ${receiverClass.simpleName}")
        } catch (securityException: SecurityException) {
            Log.w(
                LOG_TAG,
                "Failed to schedule alarm for ${receiverClass.simpleName}",
                securityException
            )
        }
    }

    fun cancel(
        context: Context,
        receiverClass: Class<out BroadcastReceiver>,
        requestCode: Int,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = createPendingIntent(
            context = context,
            receiverClass = receiverClass,
            requestCode = requestCode,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(LOG_TAG, "Cancelled daily alarm for ${receiverClass.simpleName}")
        } catch (securityException: SecurityException) {
            Log.w(
                LOG_TAG,
                "Failed to cancel alarm for ${receiverClass.simpleName}",
                securityException
            )
        }
    }

    private fun createPendingIntent(
        context: Context,
        receiverClass: Class<out BroadcastReceiver>,
        requestCode: Int,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, receiverClass)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags,
        )
    }
}
