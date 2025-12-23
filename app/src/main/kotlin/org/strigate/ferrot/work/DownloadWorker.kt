package org.strigate.ferrot.work

import android.content.Context
import android.os.Build
import android.util.Log
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
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.extensions.extractFileExtension
import org.strigate.ferrot.extensions.parseErrorMessage
import org.strigate.ferrot.extensions.toast
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

class DownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadPathProvider: DownloadPathProvider,
    private val notificationService: NotificationService,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    private var _downloadId: Long = -1L

    private var videoTitle: String? = null
    private var lastForegroundProgress: Int = -1
    private val qualityProfile: QualityProfile = QualityProfile.MAX

    override suspend fun doWork(): Result {
        _downloadId = inputData.getLong(KEY_ID, -1L)

        val downloadId = _downloadId
        val tag = "Download[$downloadId]:"
        Log.d(LOG_TAG, "$tag Starting work")

        if (runAttemptCount > 20 || downloadId <= 0L) {
            Log.w(LOG_TAG, "$tag Attempts exhausted or download ID invalid")
            return Result.failure()
        }

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
                if (!canStart) {
                    Log.w(LOG_TAG, "$tag Cannot start in status: ${download.status}")
                    return@mainScope Result.failure()
                }

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

                Log.d(LOG_TAG, "$tag Downloading metadata")
                downloadUseCase.updateDownloadStatusByIdUseCase(downloadId, DownloadStatus.METADATA)
                val videoInfo = withContext(Dispatchers.IO) {
                    youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
                }
                Log.d(LOG_TAG, "$tag Downloaded metadata")

                videoTitle = videoInfo.title?.takeIf { it.isNotBlank() } ?: download.url
                updateForeground(
                    notificationText = appContext.getString(R.string.worker_notification_text_download_in_progress),
                    indeterminate = true,
                    contentText = videoTitle,
                    extras = notificationExtras,
                )

                Log.d(LOG_TAG, "$tag Downloading thumbnail")
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
                            durationSeconds = videoInfo.duration.takeIf { it > 0 },
                        )
                    )
                }
                Log.d(LOG_TAG, "$tag Downloaded thumbnail")

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

                val weights = decideWeights(
                    videoBytes = videoInfo.fileSize.takeIf { it > 0L }
                        ?: videoInfo.fileSizeApproximate.takeIf { it > 0L },
                    audioBytes = audioByteEstimateFromDuration(videoInfo.duration),
                )

                val baseProcessId = "dl-$downloadId-${System.nanoTime()}"
                val videoProcessId = "${baseProcessId}_video"
                val audioProcessId = "${baseProcessId}_audio"

                Log.d(LOG_TAG, "$tag Decided weights: v=${weights.video}, a=${weights.audio}")
                Log.d(LOG_TAG, "$tag videoProcessId: $videoProcessId")
                Log.d(LOG_TAG, "$tag audioProcessId: $audioProcessId")

                var maxBytes = 0L
                val bytesProviderRaw = {
                    maxBytes = max(maxBytes, directoryBytesSum(uidDir))
                    maxBytes
                }
                val videoTemplate = "${uidDir.absolutePath}/%(title)s [%(id)s] - Video.%(ext)s"
                val audioTemplate = "${uidDir.absolutePath}/%(title)s [%(id)s] - Audio.%(ext)s"

                val phaseContext = PhaseContext(
                    phase = DownloadMediaType.VIDEO,
                    weights = weights,
                    title = videoTitle ?: download.url,
                    notificationExtras = notificationExtras,
                )

                Log.d(LOG_TAG, "$tag Downloading video")
                withContext(Dispatchers.IO) {
                    collectPhase(
                        processId = videoProcessId,
                        url = download.url,
                        template = videoTemplate,
                        qualityProfile = qualityProfile,
                        bytesProviderRaw = bytesProviderRaw,
                        phaseContext = phaseContext.copy(phase = DownloadMediaType.VIDEO),
                        initialVideoPercent = 0f,
                        onCanceled = { wasDownloadDeleted = true },
                        onCombined = {},
                    )
                }
                Log.d(LOG_TAG, "$tag Downloaded video")

                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    Log.w(LOG_TAG, "$tag Download record was deleted during download")
                    wasDownloadDeleted = true
                    throw CancellationException()
                }

                val videoOutputFile = locateOutputFileByInfoId(uidDir, videoInfo.id)
                if (videoOutputFile == null || !videoOutputFile.exists()) {
                    Log.w(LOG_TAG, "$tag Video output file could not be located or does not exist")
                    return@mainScope handleDownloadFailedResult()
                }

                val videoOutputFilePath = videoOutputFile.absolutePath
                val videoOutputFileExtension = videoInfo.ext ?: videoOutputFilePath
                    .extractFileExtension()
                    .orEmpty()

                Log.d(LOG_TAG, "$tag Video output file exists, saving path")
                downloadVideoUseCase.saveDownloadVideoUseCase(
                    DownloadVideo(
                        downloadId = downloadId,
                        filePath = videoOutputFilePath,
                        fileExtension = videoOutputFileExtension,
                    )
                )

                withContext(Dispatchers.IO) {
                    val bytesDownloaded = directoryBytesSum(uidDir)
                    downloadProgressUseCase.updateDownloadProgressUseCase(
                        id = downloadId,
                        progressPercent = combinedPercent(
                            phase = DownloadMediaType.VIDEO,
                            videoPhasePercent = 100f,
                            audioPhasePercent = 0f,
                            weights = weights,
                        ),
                        bytesDownloaded = bytesDownloaded,
                        etaSeconds = null,
                    )
                }

                Log.d(LOG_TAG, "$tag Downloading audio")
                withContext(Dispatchers.IO) {
                    collectPhase(
                        processId = audioProcessId,
                        url = download.url,
                        template = audioTemplate,
                        qualityProfile = qualityProfile,
                        bytesProviderRaw = bytesProviderRaw,
                        phaseContext = phaseContext.copy(phase = DownloadMediaType.AUDIO),
                        initialVideoPercent = 100f,
                        onCanceled = { wasDownloadDeleted = true },
                        onCombined = {},
                    )
                }
                Log.d(LOG_TAG, "$tag Downloaded audio")

                val audioOutputFile = locateOutputFileByInfoId(uidDir, videoInfo.id, audio = true)
                if (audioOutputFile == null || !audioOutputFile.exists()) {
                    Log.w(LOG_TAG, "$tag Audio output file could not be located or does not exist")
                } else {
                    val audioOutputFilePath = audioOutputFile.absolutePath
                    val audioOutputFileExtension = audioOutputFilePath
                        .extractFileExtension()
                        .orEmpty()

                    Log.d(LOG_TAG, "$tag Audio output file exists, saving path")
                    downloadAudioUseCase.saveDownloadAudioUseCase(
                        DownloadAudio(
                            downloadId = downloadId,
                            filePath = audioOutputFilePath,
                            fileExtension = audioOutputFileExtension,
                        )
                    )
                }

                val finalPercent = 100
                updateForeground(
                    notificationText = appContext.getString(R.string.download_complete),
                    progress = finalPercent,
                    indeterminate = false,
                    contentText = videoTitle ?: download.url,
                    extras = notificationExtras,
                )

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

                Log.d(LOG_TAG, "$tag $downloadComplete")
                appContext.toast("$downloadComplete: $contentText", true)
                notificationService.notifyDownloaded(
                    contentText = contentText,
                    contentTitle = downloadComplete,
                    extras = notificationExtras,
                )
                Result.success()

            } catch (throwable: Throwable) {
                Log.w(LOG_TAG, "$tag Caught throwable: $throwable", throwable)

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

                Log.w(LOG_TAG, "$tag stopReason=$stopReason")
                return@mainScope when (stopReason) {
                    WorkInfo.STOP_REASON_CANCELLED_BY_APP,
                    WorkInfo.STOP_REASON_USER -> {
                        Log.w(LOG_TAG, "$tag Cancel came from app or user")
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

    private fun locateOutputFileByInfoId(
        dir: File,
        infoId: String?,
        audio: Boolean = false,
    ): File? {
        if (infoId.isNullOrBlank() || !dir.exists()) return null
        val extensions = if (audio) {
            listOf("mp3", "m4a", "opus")
        } else {
            listOf("mp4", "mkv", "webm")
        }
        extensions.map { File(dir, "$infoId.$it") }
            .firstOrNull { it.exists() && it.length() > 0L }
            ?.let { return it }

        return dir.listFiles()
            ?.filter {
                it.isFile && !it.name.startsWith("thumb_") && it.length() > 0L
            }
            ?.filter {
                extensions.any { extension -> it.name.endsWith(".$extension") }
            }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun decideWeights(videoBytes: Long?, audioBytes: Long?): Weights {
        return if (videoBytes != null && audioBytes != null && videoBytes > 0 && audioBytes > 0) {
            val total = videoBytes + audioBytes
            Weights(
                video = videoBytes.toDouble() / total.toDouble(),
                audio = audioBytes.toDouble() / total.toDouble(),
            )
        } else {
            Weights(video = 0.85, audio = 0.15)
        }
    }

    private fun combinedPercent(
        phase: DownloadMediaType,
        videoPhasePercent: Float,
        audioPhasePercent: Float,
        weights: Weights,
    ): Float {
        return when (phase) {
            DownloadMediaType.VIDEO -> (videoPhasePercent.coerceIn(
                0f,
                100f
            ) * weights.video).toFloat()

            DownloadMediaType.AUDIO -> ((100f * weights.video) + (audioPhasePercent.coerceIn(
                0f,
                100f,
            ) * weights.audio)).toFloat()
        }.coerceIn(0f, 100f)
    }

    private fun audioByteEstimateFromDuration(durationSeconds: Int): Long? {
        if (durationSeconds <= 0) return null
        return durationSeconds.toLong() * 24_000L
    }

    private fun formatEta(etaSeconds: Long?): String {
        val eta = etaSeconds?.takeIf { it > 0 } ?: return ""
        val minutes = eta / 60
        val seconds = eta % 60
        return buildString {
            if (minutes > 0) append("${minutes}m")
            append("${seconds}s")
        }
    }

    private fun buildNotifLine(percentInt: Int, eta: String, title: String): String {
        val parts = mutableListOf<String>()
        parts += "$percentInt%"
        if (eta.isNotEmpty()) parts += eta
        parts += title
        return parts.joinToString(" - ")
    }

    private suspend fun collectPhase(
        processId: String,
        url: String,
        template: String,
        qualityProfile: QualityProfile,
        bytesProviderRaw: () -> Long,
        phaseContext: PhaseContext,
        initialVideoPercent: Float,
        onCanceled: () -> Unit,
        onCombined: (Float) -> Unit,
    ) {
        val throttle = ProgressThrottle()
        val downloadTickFlow = youtubeDlAndroidUseCase.downloadWithProgressUseCase(
            url = url,
            template = template,
            profile = qualityProfile,
            processId = processId,
            bytesProvider = { throttle.throttledBytes(bytesProviderRaw) },
            downloadMediaType = phaseContext.phase,
        )
        try {
            downloadTickFlow.collect { tick ->
                val status = downloadUseCase.getDownloadByIdUseCase(_downloadId)?.status
                if (status == null) {
                    onCanceled()
                    destroyYoutubeDlProcess(processId)
                    throw CancellationException()
                }
                if (status == DownloadStatus.STOPPED) {
                    destroyYoutubeDlProcess(processId)
                    throw CancellationException()
                }

                val combined = when (phaseContext.phase) {
                    DownloadMediaType.VIDEO -> combinedPercent(
                        phase = DownloadMediaType.VIDEO,
                        videoPhasePercent = tick.percent,
                        audioPhasePercent = 0f,
                        weights = phaseContext.weights,
                    )

                    DownloadMediaType.AUDIO -> combinedPercent(
                        phase = DownloadMediaType.AUDIO,
                        videoPhasePercent = initialVideoPercent,
                        audioPhasePercent = tick.percent,
                        weights = phaseContext.weights,
                    )
                }
                onCombined(combined)

                val percentInt = combined.toInt().coerceIn(0, 100)

                if (throttle.shouldPersist(percentInt)) {
                    downloadProgressUseCase.updateDownloadProgressUseCase(
                        id = _downloadId,
                        progressPercent = combined,
                        etaSeconds = tick.etaSeconds,
                        bytesDownloaded = throttle.throttledBytes(bytesProviderRaw),
                    )
                }

                if (throttle.shouldNotify(percentInt)) {
                    val etaLine = formatEta(tick.etaSeconds)
                    val contentLine = buildNotifLine(percentInt, etaLine, phaseContext.title)
                    updateForeground(
                        notificationText = appContext.getString(R.string.worker_notification_text_download_in_progress),
                        progress = percentInt,
                        indeterminate = false,
                        contentText = contentLine,
                        extras = phaseContext.notificationExtras,
                    )
                    lastForegroundProgress = percentInt
                }
            }
        } finally {
            destroyYoutubeDlProcess(processId)
        }
    }

    private data class PhaseContext(
        val phase: DownloadMediaType,
        val weights: Weights,
        val title: String,
        val notificationExtras: Map<String, String>,
    )

    private class ProgressThrottle(
        private val minPercentDelta: Int = 1,
        private val minDbMillis: Long = 200,
        private val minFsMillis: Long = 300,
    ) {
        private var lastNotifPercent = -1
        private var lastDbPercent = -1
        private var lastDbAt = 0L
        private var lastBytesAt = 0L
        private var cachedBytes = 0L

        fun shouldNotify(percentInt: Int): Boolean {
            if (percentInt != lastNotifPercent) {
                lastNotifPercent = percentInt
                return true
            }
            return false
        }

        fun shouldPersist(percentInt: Int, now: Long = System.currentTimeMillis()): Boolean {
            if (percentInt - lastDbPercent >= minPercentDelta || now - lastDbAt >= minDbMillis) {
                lastDbPercent = percentInt
                lastDbAt = now
                return true
            }
            return false
        }

        fun throttledBytes(readDirBytes: () -> Long, now: Long = System.currentTimeMillis()): Long {
            if (now - lastBytesAt >= minFsMillis) {
                cachedBytes = readDirBytes()
                lastBytesAt = now
            }
            return cachedBytes
        }
    }

    private data class Weights(val video: Double, val audio: Double)

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
