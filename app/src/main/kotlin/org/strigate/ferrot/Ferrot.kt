package org.strigate.ferrot

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.di.WorkerFactory
import org.strigate.ferrot.app.receiver.AirplaneModeReceiver
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.work.DeleteAllDuplicateDownloadsWorker
import org.strigate.ferrot.work.DeleteAllOrphanDownloadFilesWorker
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import org.strigate.ferrot.work.UpdateDependenciesWorker
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
    lateinit var settingsUseCase: SettingsUseCase

    override val workManagerConfiguration: Configuration
        get() {
            return Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
        notificationService.initializeNotificationChannels()
        registerReceivers()
        analyticsLogger.setConsent(!BuildConfig.DEBUG)

        applicationScope.launch {
            runCatching {
                enqueueWork()
                Log.i(LOG_TAG, "Enqueued background work")
            }.onFailure {
                Log.w(LOG_TAG, "Failed to enqueue background work", it)
            }
        }
    }

    private fun registerReceivers() {
        registerReceiver(
            AirplaneModeReceiver(),
            IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
        )
    }

    private suspend fun enqueueWork() {
        val appContext = this@Ferrot

        val settings = combine(
            settingsUseCase.getAutomaticUpdatesSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDependencyUpdatesSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase(),
        ) { automaticUpdates, automaticDependencyUpdates, automaticDuplicateDownloadDeletion ->
            EnqueuedWorkSettings(
                automaticUpdates = automaticUpdates,
                automaticDependencyUpdates = automaticDependencyUpdates,
                automaticDuplicateDownloadDeletion = automaticDuplicateDownloadDeletion,
            )
        }.first()

        if (settings.automaticUpdates) {
            DownloadAvailableUpdateWorker.enqueuePeriodicKeep(appContext)
        } else {
            DownloadAvailableUpdateWorker.cancelPeriodic(appContext)
        }
        if (settings.automaticDependencyUpdates) {
            UpdateDependenciesWorker.enqueuePeriodicKeep(appContext)
        } else {
            UpdateDependenciesWorker.cancelPeriodic(appContext)
        }
        if (settings.automaticDuplicateDownloadDeletion) {
            DeleteAllDuplicateDownloadsWorker.enqueuePeriodicKeep(appContext)
        } else {
            DeleteAllDuplicateDownloadsWorker.cancelPeriodic(appContext)
        }
        DeleteAllOrphanDownloadFilesWorker.enqueuePeriodicKeep(appContext)
    }

    private data class EnqueuedWorkSettings(
        val automaticUpdates: Boolean,
        val automaticDependencyUpdates: Boolean,
        val automaticDuplicateDownloadDeletion: Boolean,
    )
}
