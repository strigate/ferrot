package org.strigate.ferrot

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.di.WorkerFactory
import org.strigate.ferrot.app.receiver.AirplaneModeReceiver
import org.strigate.ferrot.domain.usecase.combined.ConfigureBackgroundWorkUseCase
import javax.inject.Inject

@HiltAndroidApp
class Ferrot : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var configureBackgroundWorkUseCase: ConfigureBackgroundWorkUseCase

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        notificationService.initializeNotificationChannels()
        registerReceivers()
        analyticsLogger.setConsent(!BuildConfig.DEBUG)

        applicationScope.launch {
            runCatching {
                configureWork()
                Log.i(LOG_TAG, "Configured background work")
            }.onFailure {
                Log.w(LOG_TAG, "Failed to configure background work", it)
            }
        }
    }

    private fun registerReceivers() {
        registerReceiver(
            AirplaneModeReceiver(),
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
        )
    }

    private suspend fun configureWork() {
        configureBackgroundWorkUseCase()
    }
}
