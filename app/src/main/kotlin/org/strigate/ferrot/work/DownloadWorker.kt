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
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.KEY_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DOWNLOAD
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.DownloadMediaType
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

    private var videoTitle: String? = null
    private var lastForegroundProgress: Int = -1
    private val qualityProfile: QualityProfile = QualityProfile.MAX

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_ID, -1L)
        _downloadId = downloadId

        if (runAttemptCount > 20 || downloadId <= 0L) return Result.failure()

        val download = downloadUseCase.getDownloadByIdUseCase(downloadId)
            ?: return handleDownloadFailedResult()

        val notificationExtras = mapOf(
            EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOAD,
            EXTRA_DOWNLOAD_ID to download.id.toString(),
        )
        enableForeground(
            notificationId = downloadId,
            notificationText = appContext.getString(R.string.worker_notification_text_download_in_progress),
            indeterminate = true,
            contentText = download.url,
            extras = notificationExtras,
        )

        var wasDownloadDeleted = false
        return coroutineScope mainScope@{
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
                if (!uidDir.exists() && !uidDir.mkdirs()) {
                    return@mainScope Result.failure()
                }

                downloadUseCase.updateDownloadStatusByIdUseCase(downloadId, DownloadStatus.METADATA)
                val videoInfo = withContext(Dispatchers.IO) {
                    youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
                }

                videoTitle = videoInfo.title?.takeIf { it.isNotBlank() } ?: download.url
                updateForeground(
                    notificationText = appContext.getString(R.string.worker_notification_text_download_in_progress),
                    indeterminate = true,
                    contentText = videoTitle,
                    extras = notificationExtras,
                )
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
                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    wasDownloadDeleted = true
                    throw CancellationException()
                }
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
                    val template = "${uidDir.absolutePath}/%(id)s.%(ext)s"
                    val downloadTickFlow = youtubeDlAndroidUseCase.downloadWithProgressUseCase(
                        url = download.url,
                        template = template,
                        profile = qualityProfile,
                        processId = processId,
                        bytesProvider = bytesProvider,
                        downloadMediaType = DownloadMediaType.VIDEO,
                    )
                    try {
                        downloadTickFlow.collect { downloadTick ->
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

                                val percentInt = downloadTick.percent.toInt().coerceIn(0, 100)
                                if (lastForegroundProgress == -1 && percentInt > 0) {
                                    val eta = downloadTick.etaSeconds
                                        ?.takeIf { it > 0 }
                                        ?.let { total ->
                                            val minutes = total / 60
                                            val seconds = total % 60
                                            buildString {
                                                if (minutes > 0) append("${minutes}m")
                                                append("${seconds}s")
                                            }
                                        } ?: ""

                                    val parts = mutableListOf<String>()
                                    parts += "$percentInt%"
                                    if (eta.isNotEmpty()) {
                                        parts += eta
                                    }
                                    parts += (videoTitle ?: download.url)
                                    val contentLine = parts.joinToString(" - ")

                                    updateForeground(
                                        notificationText = appContext.getString(
                                            R.string.worker_notification_text_download_in_progress,
                                        ),
                                        progress = percentInt,
                                        indeterminate = false,
                                        contentText = contentLine,
                                        extras = notificationExtras,
                                    )
                                    lastForegroundProgress = percentInt
                                } else if (percentInt != lastForegroundProgress) {
                                    lastForegroundProgress = percentInt

                                    val eta = downloadTick.etaSeconds
                                        ?.takeIf { it > 0 }
                                        ?.let { total ->
                                            val minutes = total / 60
                                            val seconds = total % 60
                                            buildString {
                                                if (minutes > 0) append("${minutes}m")
                                                append("${seconds}s")
                                            }
                                        } ?: ""

                                    val parts = mutableListOf<String>()
                                    parts += "$percentInt%"
                                    if (eta.isNotEmpty()) {
                                        parts += eta
                                    }
                                    parts += (videoTitle ?: download.url)
                                    val contentLine = parts.joinToString(" - ")

                                    updateForeground(
                                        notificationText = appContext.getString(
                                            R.string.worker_notification_text_download_in_progress,
                                        ),
                                        progress = percentInt,
                                        indeterminate = false,
                                        contentText = contentLine,
                                        extras = notificationExtras,
                                    )
                                }
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

                val outputFile = locateOutputFileByInfoId(uidDir, videoInfo.id)
                if (outputFile == null) {
                    Log.d(LOG_TAG, "Output file could not be located after download")
                    return@mainScope handleDownloadFailedResult()
                }
                downloadUseCase.updateDownloadFilePathUseCase(
                    id = downloadId,
                    fileName = outputFile.absolutePath,
                )

                withContext(Dispatchers.IO) {
                    val bytesDownloaded = directoryBytesSum(uidDir)
                    downloadProgressUseCase.updateDownloadProgressUseCase(
                        id = downloadId,
                        progressPercent = 100f,
                        bytesDownloaded = bytesDownloaded,
                        etaSeconds = null,
                    )
                }
                run {
                    val parts = mutableListOf<String>()
                    parts += "100%"
                    parts += (videoTitle ?: download.url)
                    val finalLine = parts.joinToString(" - ")
                    updateForeground(
                        notificationText = appContext.getString(R.string.download_complete),
                        progress = 100,
                        indeterminate = false,
                        contentText = finalLine,
                        extras = notificationExtras,
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
                val contentText = videoTitle ?: download.url

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

    private fun locateOutputFileByInfoId(dir: File, infoId: String?): File? {
        if (infoId.isNullOrBlank() || !dir.exists()) {
            return null
        }
        val preferredExtensions = listOf("mp4", "mkv", "webm", "mp3", "m4a", "opus")
        preferredExtensions.map { File(dir, "$infoId.$it") }
            .firstOrNull { it.exists() && it.length() > 0L }
            ?.let { return it }
        val file = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("$infoId.") && it.length() > 0L }
            ?.maxByOrNull { it.lastModified() }
        return file
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
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(id))
        }

        private fun uniqueWorkName(downloadId: Long): String = "$ONETIME_DOWNLOAD-$downloadId"
    }
}
