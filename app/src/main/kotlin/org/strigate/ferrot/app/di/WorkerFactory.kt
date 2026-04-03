package org.strigate.ferrot.app.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.combined.GetPendingDownloadsCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.work.DeleteAllDuplicateDownloadsWorker
import org.strigate.ferrot.work.DeleteAllOrphanDownloadFilesWorker
import org.strigate.ferrot.work.DeleteDownloadsWorker
import org.strigate.ferrot.work.DeletePendingDownloadDelayedWorker
import org.strigate.ferrot.work.DeletePendingDownloadsDelayedWorker
import org.strigate.ferrot.work.DeletePendingDownloadsImmediateWorker
import org.strigate.ferrot.work.DownloadAvailableUpdateWorker
import org.strigate.ferrot.work.DownloadWorker
import org.strigate.ferrot.work.RequeuePendingDownloadsWorker
import org.strigate.ferrot.work.UpdateDependenciesWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerFactory @Inject constructor(
    private val hiltWorkerFactory: HiltWorkerFactory,
    private val analyticsLogger: AnalyticsLogger,
    private val notificationService: NotificationService,
    private val stateUseCase: StateUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val updatePathProvider: UpdatePathProvider,
    private val downloadPathProvider: DownloadPathProvider,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val stopDownloadUseCase: StopDownloadUseCase,
    private val getPendingDownloadsCombinedUseCase: GetPendingDownloadsCombinedUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            DownloadAvailableUpdateWorker::class.java.name -> {
                DownloadAvailableUpdateWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    notificationService = notificationService,
                    stateUseCase = stateUseCase,
                    updatePathProvider = updatePathProvider,
                    availableUpdateUseCase = availableUpdateUseCase,
                )
            }

            UpdateDependenciesWorker::class.java.name -> {
                UpdateDependenciesWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    stateUseCase = stateUseCase,
                )
            }

            DownloadWorker::class.java.name -> {
                DownloadWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    analyticsLogger = analyticsLogger,
                    notificationService = notificationService,
                    settingsUseCase = settingsUseCase,
                    downloadPathProvider = downloadPathProvider,
                    youtubeDlAndroidUseCase = youtubeDlAndroidUseCase,
                    downloadUseCase = downloadUseCase,
                    downloadVideoUseCase = downloadVideoUseCase,
                    downloadAudioUseCase = downloadAudioUseCase,
                    downloadMetadataUseCase = downloadMetadataUseCase,
                    downloadProgressUseCase = downloadProgressUseCase,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                )
            }

            RequeuePendingDownloadsWorker::class.java.name -> {
                RequeuePendingDownloadsWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    getPendingDownloadsCombinedUseCase = getPendingDownloadsCombinedUseCase,
                    startDownloadUseCase = startDownloadUseCase,
                )
            }

            DeleteDownloadsWorker::class.java.name -> {
                DeleteDownloadsWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                    stopDownloadUseCase = stopDownloadUseCase,
                )
            }

            DeletePendingDownloadsImmediateWorker::class.java.name -> {
                DeletePendingDownloadsImmediateWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    downloadUseCase = downloadUseCase,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                    stopDownloadUseCase = stopDownloadUseCase,
                )
            }

            DeletePendingDownloadsDelayedWorker::class.java.name -> {
                DeletePendingDownloadsDelayedWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    downloadUseCase = downloadUseCase,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                    stopDownloadUseCase = stopDownloadUseCase,
                )
            }

            DeletePendingDownloadDelayedWorker::class.java.name -> {
                DeletePendingDownloadDelayedWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    downloadUseCase = downloadUseCase,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                    stopDownloadUseCase = stopDownloadUseCase,
                )
            }

            DeleteAllDuplicateDownloadsWorker::class.java.name -> {
                DeleteAllDuplicateDownloadsWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    downloadUseCase = downloadUseCase,
                    downloadVideoUseCase = downloadVideoUseCase,
                    downloadMetadataUseCase = downloadMetadataUseCase,
                    deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
                )
            }

            DeleteAllOrphanDownloadFilesWorker::class.java.name -> {
                DeleteAllOrphanDownloadFilesWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    downloadPathProvider = downloadPathProvider,
                    downloadAudioUseCase = downloadAudioUseCase,
                    downloadVideoUseCase = downloadVideoUseCase,
                    downloadMetadataUseCase = downloadMetadataUseCase,
                )
            }

            else -> hiltWorkerFactory.createWorker(
                appContext = appContext,
                workerClassName = workerClassName,
                workerParameters = workerParameters,
            )
        }
    }
}
