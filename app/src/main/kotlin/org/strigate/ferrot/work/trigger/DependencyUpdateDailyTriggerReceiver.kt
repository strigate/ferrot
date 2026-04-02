package org.strigate.ferrot.work.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_UPDATE_DEPENDENCIES
import org.strigate.ferrot.app.alarm.DailyWorkAlarmScheduler
import org.strigate.ferrot.work.worker.UpdateDependenciesWorker

class DependencyUpdateDailyTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        UpdateDependenciesWorker.enqueueOneTimeReplace(context)
        schedule(context)
    }

    companion object {
        private const val REQUEST_CODE = 3002
        private const val TARGET_HOUR = 4

        fun schedule(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_UPDATE_DEPENDENCIES)
            DailyWorkAlarmScheduler.schedule(
                context = context,
                receiverClass = DependencyUpdateDailyTriggerReceiver::class.java,
                requestCode = REQUEST_CODE,
                targetHour = TARGET_HOUR,
            )
        }

        fun cancel(context: Context) {
            DailyWorkAlarmScheduler.cancel(
                context = context,
                receiverClass = DependencyUpdateDailyTriggerReceiver::class.java,
                requestCode = REQUEST_CODE,
            )
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_UPDATE_DEPENDENCIES)
        }
    }
}
