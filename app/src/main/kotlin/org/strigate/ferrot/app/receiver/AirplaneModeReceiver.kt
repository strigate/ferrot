package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.work.RequeuePendingDownloadsWorker

@AndroidEntryPoint
class AirplaneModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            return
        }
        val state = intent.getBooleanExtra("state", false)
        Log.d(LOG_TAG, "Airplane mode: $state")
        if (!state) {
            RequeuePendingDownloadsWorker.enqueueOneItem(context)
        }
    }
}
