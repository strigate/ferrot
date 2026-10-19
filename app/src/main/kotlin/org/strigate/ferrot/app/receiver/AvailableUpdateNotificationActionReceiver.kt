package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.Extras.EXTRA_NOTIFICATION_ACTION
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.AvailableUpdateNotificationActionType
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import javax.inject.Inject

@AndroidEntryPoint
class AvailableUpdateNotificationActionReceiver : BroadcastReceiver() {
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val ioScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    @Inject
    lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    @Inject
    lateinit var notificationService: NotificationService

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        ioScope.launch {
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
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(LOG_TAG, "Failed to handle notification action", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
