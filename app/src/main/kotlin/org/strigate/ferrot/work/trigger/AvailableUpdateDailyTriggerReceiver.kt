package org.strigate.ferrot.work.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DOWNLOAD_AVAILABLE_UPDATE
import org.strigate.ferrot.app.alarm.DailyWorkAlarmScheduler
import org.strigate.ferrot.work.worker.DownloadAvailableUpdateWorker

class AvailableUpdateDailyTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        DownloadAvailableUpdateWorker.enqueueOneTimeReplace(context)
        schedule(context)
    }

    companion object {
        private const val REQUEST_CODE = 3001
        private const val TARGET_HOUR = 3

        fun schedule(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_DOWNLOAD_AVAILABLE_UPDATE)
            DailyWorkAlarmScheduler.schedule(
                context = context,
                receiverClass = AvailableUpdateDailyTriggerReceiver::class.java,
                requestCode = REQUEST_CODE,
                targetHour = TARGET_HOUR,
            )
        }

        fun cancel(context: Context) {
            DailyWorkAlarmScheduler.cancel(
                context = context,
                receiverClass = AvailableUpdateDailyTriggerReceiver::class.java,
                requestCode = REQUEST_CODE,
            )
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_DOWNLOAD_AVAILABLE_UPDATE)
        }
    }
}
