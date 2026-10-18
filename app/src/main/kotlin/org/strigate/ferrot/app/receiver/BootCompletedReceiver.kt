package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.work.DeleteAllDuplicateDownloadsWorker
import org.strigate.ferrot.work.RequeueResumableDownloadsWorker
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val ioScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

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
        val pendingResult = goAsync()
        ioScope.launch {
            try {
                val bootTimeMillis = stateUseCase.getBootTimeMillisUseCase().firstOrNull()
                if (bootTimeMillis == null || abs(bootTimeMillis - currentBootTimeMillis) > 5_000) {
                    stateUseCase.saveBootTimeMillisUseCase(currentBootTimeMillis)
                    Log.d(LOG_TAG, "Boot completed received")

                    RequeueResumableDownloadsWorker.enqueueOneTime(context)
                    val automaticDuplicateDownloadDeletionEnabled = settingsUseCase
                        .getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase()
                        .first()

                    if (automaticDuplicateDownloadDeletionEnabled) {
                        DeleteAllDuplicateDownloadsWorker.enqueueDebouncedReplace(context)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(LOG_TAG, "Failed to handle boot completion", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
