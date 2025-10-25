package org.strigate.ferrot.work

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.DOWNLOAD
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.KEY_WIFI_ONLY
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.extensions.parseErrorMessage
import org.strigate.ferrot.extensions.toast
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadPathProvider: DownloadPathProvider,
    private val notificationService: NotificationService,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    private var _downloadId: Long = -1L

    private val qualityProfile: QualityProfile = QualityProfile.MAX

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_ID, -1L)
        _downloadId = downloadId

        if (runAttemptCount > 20 || downloadId <= 0L) return Result.failure()
        enableForeground(
            notificationText = appContext.getString(R.string.worker_notification_text_download_in_progress),
        )
        val download = downloadUseCase.getDownloadByIdUseCase(downloadId)
            ?: return handleDownloadFailedResult()

        var wasDownloadDeleted = false
        return coroutineScope mainScope@{
            val notificationExtras = mapOf(
                EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOAD,
                EXTRA_DOWNLOAD_ID to download.id.toString(),
            )
            try {
                val canStart = when (download.status) {
                    DownloadStatus.QUEUED,
                    DownloadStatus.WAITING_FOR_NETWORK,
                    DownloadStatus.WAITING_FOR_WIFI,
                    DownloadStatus.PAUSED,
                    DownloadStatus.FAILED,
                    DownloadStatus.METADATA,
                    DownloadStatus.DOWNLOADING -> true

                    else -> false
                }
                if (!canStart) return@mainScope Result.failure()

                analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_STARTED)

                resetProgressAndCleanup()
                downloadUseCase.updateDownloadErrorMessageUseCase(downloadId, null)
                downloadUseCase.updateDownloadStartedAtUseCase(
                    startedAtMillis = System.currentTimeMillis(),
                    id = downloadId,
                )

                val uidDir = downloadPathProvider.uidDir(download.uid)
                downloadUseCase.updateDownloadStatusByIdUseCase(downloadId, DownloadStatus.METADATA)
                val videoInfo = withContext(Dispatchers.IO) {
                    youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
                }

                withContext(Dispatchers.IO) {
                    val thumbnailFilePath = youtubeDlAndroidUseCase.downloadThumbnailUseCase(
                        url = download.url,
                        outputDir = uidDir,
                        videoId = videoInfo.id,
                    )
                    if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                        wasDownloadDeleted = true
                        throw CancellationException()
                    }
                    downloadMetadataUseCase.saveDownloadMetadataUseCase(
                        DownloadMetadata(
                            downloadId = downloadId,
                            title = videoInfo.title,
                            thumbnailFilePath = thumbnailFilePath,
                        )
                    )
                }

                val expectedBytes = when {
                    videoInfo.fileSize > 0L -> videoInfo.fileSize
                    videoInfo.fileSizeApproximate > 0L -> videoInfo.fileSizeApproximate
                    else -> null
                }
                if (expectedBytes != null) {
                    downloadProgressUseCase.updateDownloadExpectedBytesUseCase(
                        expectedBytes = expectedBytes,
                        id = downloadId,
                    )
                }

                val template = "${uidDir.absolutePath}/%(id)s.%(ext)s"
                val outputPath = withContext(Dispatchers.IO) {
                    youtubeDlAndroidUseCase.resolveOutputPathUseCase(
                        url = download.url,
                        template = template,
                        qualityProfile = qualityProfile,
                    )
                }

                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    wasDownloadDeleted = true
                    throw CancellationException()
                }
                downloadUseCase.updateDownloadFilePathUseCase(downloadId, outputPath)
                downloadUseCase.updateDownloadStatusByIdUseCase(
                    status = DownloadStatus.DOWNLOADING,
                    id = downloadId,
                )

                val processId = "dl-$downloadId-${System.nanoTime()}"
                var maxBytes = 0L
                val bytesProvider = {
                    maxBytes = max(maxBytes, directoryBytesSum(uidDir))
                    maxBytes
                }
                withContext(Dispatchers.IO) {
                    val flow = youtubeDlAndroidUseCase.downloadWithProgressUseCase(
                        url = download.url,
                        template = template,
                        profile = qualityProfile,
                        processId = processId,
                        bytesProvider = bytesProvider,
                    )
                    try {
                        flow.collect { downloadTick ->
                            if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                                wasDownloadDeleted = true
                                destroyYoutubeDlProcess(processId)
                                throw CancellationException()
                            }
                            val downloadStatus = downloadUseCase
                                .getDownloadByIdUseCase(downloadId)
                                ?.status

                            if (downloadStatus == DownloadStatus.STOPPED) {
                                destroyYoutubeDlProcess(processId)
                                throw CancellationException()
                            }
                            if (downloadStatus != DownloadStatus.COMPLETED && downloadStatus != DownloadStatus.FAILED) {
                                downloadProgressUseCase.updateDownloadProgressUseCase(
                                    id = downloadId,
                                    progressPercent = downloadTick.percent,
                                    etaSeconds = downloadTick.etaSeconds,
                                    bytesDownloaded = downloadTick.bytesDownloaded,
                                )
                            }
                        }
                    } finally {
                        destroyYoutubeDlProcess(processId)
                    }
                }

                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    wasDownloadDeleted = true
                    throw CancellationException()
                }
                val outputFile = File(outputPath)
                if (!outputFile.exists() || outputFile.length() <= 0L) {
                    return@mainScope handleDownloadFailedResult()
                }

                withContext(Dispatchers.IO) {
                    val bytesDownloaded = directoryBytesSum(uidDir)
                    downloadProgressUseCase.updateDownloadProgressUseCase(
                        id = downloadId,
                        progressPercent = 100f,
                        bytesDownloaded = bytesDownloaded,
                        etaSeconds = null,
                    )
                }

                downloadUseCase.updateDownloadErrorMessageUseCase(downloadId, null)
                downloadUseCase.updateDownloadStatusByIdUseCase(
                    status = DownloadStatus.COMPLETED,
                    id = downloadId,
                )
                downloadUseCase.updateDownloadCompletedAtUseCase(
                    completedAtMillis = System.currentTimeMillis(),
                    id = downloadId,
                )
                analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_COMPLETED)

                val downloadComplete = appContext.getString(R.string.download_complete)
                val contentText = videoInfo.title?.takeIf { it.isNotBlank() } ?: download.url

                Log.d(LOG_TAG, downloadComplete)
                appContext.toast("$downloadComplete: $contentText", true)
                notificationService.notifyDownloaded(
                    contentText = contentText,
                    contentTitle = downloadComplete,
                    extras = notificationExtras,
                )
                Result.success()

            } catch (throwable: Throwable) {
                Log.w(LOG_TAG, "Caught throwable: $throwable", throwable)

                suspend fun handleDownloadFailure() = handleDownloadFailure(
                    throwable = throwable,
                    notificationText = download.url,
                    notificationExtras = notificationExtras,
                )

                val isCancellation = throwable is CancellationException ||
                        throwable is YoutubeDL.CanceledException

                if (!isCancellation) {
                    return@mainScope handleDownloadFailure()
                }
                if (wasDownloadDeleted) {
                    return@mainScope handleDeletedDownloadResult()
                }
                if (Build.VERSION.SDK_INT < 31) {
                    return@mainScope handleDownloadFailure()
                }

                Log.w(LOG_TAG, "stopReason=$stopReason")
                return@mainScope when (stopReason) {
                    WorkInfo.STOP_REASON_CANCELLED_BY_APP,
                    WorkInfo.STOP_REASON_USER -> {
                        Log.w(LOG_TAG, "Cancel came from app or user")
                        handleDownloadStoppedResult()
                    }

                    else -> handleDownloadFailure()
                }
            }
        }
    }

    private suspend fun handleDownloadFailure(
        throwable: Throwable,
        notificationText: String,
        notificationExtras: Map<String, String>,
    ): Result {
        val downloadId = _downloadId
        if (downloadId <= 0L) {
            return Result.failure()
        }
        withContext(NonCancellable) {
            runCatching {
                downloadUseCase.updateDownloadErrorMessageUseCase(
                    errorMessage = throwable.parseErrorMessage(),
                    id = downloadId,
                )
            }
        }
        val downloadFailed = appContext.getString(R.string.download_failed)
        appContext.toast(downloadFailed, true)
        notificationService.notifyDownloaded(
            contentTitle = downloadFailed,
            contentText = notificationText,
            extras = notificationExtras,
            tag = notificationText,
        )
        return handleDownloadFailedResult()
    }

    private suspend fun handleDownloadStoppedResult(): Result {
        val downloadId = _downloadId
        if (downloadId > 0L) {
            withContext(NonCancellable) {
                val status = runCatching {
                    downloadUseCase.getDownloadByIdUseCase(downloadId)?.status
                }.getOrNull()

                val shouldPreserve = status == DownloadStatus.WAITING_FOR_WIFI ||
                        status == DownloadStatus.WAITING_FOR_NETWORK

                if (!shouldPreserve && status != DownloadStatus.COMPLETED && status != DownloadStatus.FAILED) {
                    resetProgressAndCleanup()
                    runCatching {
                        downloadUseCase.updateDownloadStatusByIdUseCase(
                            downloadId,
                            DownloadStatus.STOPPED,
                        )
                    }
                }
            }
        }
        return Result.failure()
    }

    private suspend fun handleDownloadFailedResult(): Result {
        val downloadId = _downloadId
        if (downloadId > 0L) {
            withContext(NonCancellable) {
                resetProgressAndCleanup()
                runCatching {
                    analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_FAILED)
                    downloadUseCase.updateDownloadStatusByIdUseCase(
                        downloadId,
                        DownloadStatus.FAILED,
                    )
                }
            }
        }
        return Result.failure()
    }

    private suspend fun handleDeletedDownloadResult(): Result {
        val downloadId = _downloadId
        if (downloadId <= 0L) {
            return Result.success()
        }
        runCatching {
            deleteDownloadAndRelatedCombinedUseCase(downloadId)
        }
        return Result.success()
    }

    private suspend fun resetProgressAndCleanup() {
        val downloadId = _downloadId
        if (downloadId > 0L) {
            withContext(NonCancellable) {
                runCatching {
                    downloadUseCase.deleteDownloadFilesUseCase(downloadId)
                }
                runCatching {
                    downloadProgressUseCase.updateDownloadProgressUseCase(
                        id = downloadId,
                        progressPercent = 0f,
                        bytesDownloaded = 0L,
                        etaSeconds = null,
                    )
                }
            }
        }
    }

    private fun directoryBytesSum(directory: File): Long {
        return directory.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
    }

    private fun destroyYoutubeDlProcess(processId: String) {
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
    }

    companion object {
        fun enqueueOneTimeReplace(context: Context, id: Long, wifiOnly: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) {
                        NetworkType.UNMETERED
                    } else {
                        NetworkType.CONNECTED
                    },
                )
                .build()

            val inputData = Data.Builder()
                .putLong(KEY_ID, id)
                .putBoolean(KEY_WIFI_ONLY, wifiOnly)
                .build()

            val oneTimeWorkRequestBuilder = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(id),
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequestBuilder,
            )
        }

        fun cancelUnique(context: Context, id: Long) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(uniqueWorkName(id))
        }

        private fun uniqueWorkName(downloadId: Long): String = "$DOWNLOAD-$downloadId"
    }
}
