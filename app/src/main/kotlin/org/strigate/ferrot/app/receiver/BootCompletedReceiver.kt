package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.work.RequeuePendingDownloadsWorker
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var stateUseCase: StateUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val currentBootTimeMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        CoroutineScope(Dispatchers.IO).launch {
            val bootTimeMillis = stateUseCase.getBootTimeMillisUseCase().firstOrNull()
            if (bootTimeMillis == null || abs(bootTimeMillis - currentBootTimeMillis) > 5_000) {
                stateUseCase.saveBootTimeMillisUseCase(currentBootTimeMillis)
                RequeuePendingDownloadsWorker.enqueueOneItem(context)
            }
        }
    }
}
