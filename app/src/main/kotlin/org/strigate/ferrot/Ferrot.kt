package org.strigate.ferrot

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.di.WorkerFactory
import org.strigate.ferrot.app.receiver.AirplaneModeReceiver
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject

@HiltAndroidApp
class Ferrot : Application(), Configuration.Provider, DefaultLifecycleObserver {
    @Inject
    lateinit var workerFactory: WorkerFactory

    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    @Inject
    lateinit var notificationService: NotificationService

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        WorkManager.initialize(this, workManagerConfiguration)
        notificationService.initializeNotificationChannels()
        registerReceivers()
        analyticsLogger.setConsent(!BuildConfig.DEBUG)
        enqueueWork()
    }

    private fun registerReceivers() {
        registerReceiver(
            AirplaneModeReceiver(),
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
        )
    }

    private fun enqueueWork() {
        DownloadAvailableUpdateWorker.enqueuePeriodic(this)
        UpdateDependenciesWorker.enqueuePeriodic(this)
    }
}
