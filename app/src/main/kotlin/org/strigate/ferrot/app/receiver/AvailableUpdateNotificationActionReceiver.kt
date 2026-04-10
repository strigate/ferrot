package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.Extras.EXTRA_NOTIFICATION_ACTION
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.AvailableUpdateNotificationActionType
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import javax.inject.Inject

@AndroidEntryPoint
class AvailableUpdateNotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.getStringExtra(EXTRA_NOTIFICATION_ACTION)?.let {
                    runCatching { AvailableUpdateNotificationActionType.valueOf(it) }.getOrNull()
                }
                when (action) {
                    AvailableUpdateNotificationActionType.DELETE -> {
                        availableUpdateUseCase.clearAvailableUpdateFilesAndDataUseCase()
                        notificationService.clearAvailableUpdateNotification()
                    }

                    null -> Unit
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
