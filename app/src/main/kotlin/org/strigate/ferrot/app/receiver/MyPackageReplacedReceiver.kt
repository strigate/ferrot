package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.work.RequeuePendingDownloadsWorker
import javax.inject.Inject

@AndroidEntryPoint
class MyPackageReplacedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var updatePathProvider: UpdatePathProvider

    @Inject
    lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching {
                availableUpdateUseCase.clearAvailableUpdateUseCase()
                updatePathProvider.updatesDir().deleteRecursively()
                RequeuePendingDownloadsWorker.enqueueOneItem(context)
            }
            pendingResult.finish()
        }
    }
}
