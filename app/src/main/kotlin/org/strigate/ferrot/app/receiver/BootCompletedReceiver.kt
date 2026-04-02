package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.work.trigger.AvailableUpdateDailyTriggerReceiver
import org.strigate.ferrot.work.trigger.DependencyUpdateDailyTriggerReceiver
import org.strigate.ferrot.work.worker.DeleteAllDuplicateDownloadsWorker
import org.strigate.ferrot.work.worker.RequeuePendingDownloadsWorker
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var stateUseCase: StateUseCase

    @Inject
    lateinit var settingsUseCase: SettingsUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }
        val currentBootTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        CoroutineScope(Dispatchers.IO).launch {
            val bootTimeMillis = stateUseCase.getBootTimeMillisUseCase().firstOrNull()
            if (bootTimeMillis == null || abs(bootTimeMillis - currentBootTimeMillis) > 5_000) {
                stateUseCase.saveBootTimeMillisUseCase(currentBootTimeMillis)
                Log.d(LOG_TAG, "Boot completed received")

                RequeuePendingDownloadsWorker.enqueueOneTime(context)
                val automaticUpdatesSetting = settingsUseCase
                    .getAutomaticUpdatesSettingAsFlowUseCase()
                    .first()
                val automaticDependencyUpdatesSetting = settingsUseCase
                    .getAutomaticDependencyUpdatesSettingAsFlowUseCase()
                    .first()
                val automaticDuplicateDownloadDeletionSetting = settingsUseCase
                    .getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase()
                    .first()

                if (automaticUpdatesSetting) {
                    AvailableUpdateDailyTriggerReceiver.schedule(context)
                } else {
                    AvailableUpdateDailyTriggerReceiver.cancel(context)
                }
                if (automaticDependencyUpdatesSetting) {
                    DependencyUpdateDailyTriggerReceiver.schedule(context)
                } else {
                    DependencyUpdateDailyTriggerReceiver.cancel(context)
                }
                if (automaticDuplicateDownloadDeletionSetting) {
                    DeleteAllDuplicateDownloadsWorker.enqueueDebouncedReplace(context)
                }
            }
        }
    }
}
