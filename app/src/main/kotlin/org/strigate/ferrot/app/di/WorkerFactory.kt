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
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.combined.GetPendingDownloadsCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
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
    private val updatePathProvider: UpdatePathProvider,
    private val downloadPathProvider: DownloadPathProvider,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val getPendingDownloadsCombinedUseCase: GetPendingDownloadsCombinedUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
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
                    updatePathProvider = updatePathProvider,
                    availableUpdateUseCase = availableUpdateUseCase,
                )
            }

            UpdateDependenciesWorker::class.java.name -> {
                UpdateDependenciesWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                )
            }

            DownloadWorker::class.java.name -> {
                DownloadWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    analyticsLogger = analyticsLogger,
                    downloadPathProvider = downloadPathProvider,
                    notificationService = notificationService,
                    youtubeDlAndroidUseCase = youtubeDlAndroidUseCase,
                    downloadUseCase = downloadUseCase,
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

            else -> hiltWorkerFactory.createWorker(
                appContext = appContext,
                workerClassName = workerClassName,
                workerParameters = workerParameters,
            )
        }
    }
}
