package org.strigate.ferrot.work

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.KEY_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DOWNLOAD
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.DownloadNotificationActionType
import org.strigate.ferrot.app.actions.buildDownloadNotificationAction
import org.strigate.ferrot.app.actions.buildShareDownloadNotificationAction
import org.strigate.ferrot.app.actions.downloadNotificationExtras
import org.strigate.ferrot.app.actions.downloadNotificationTag
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
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.extensions.extractFileExtension
import org.strigate.ferrot.extensions.parseErrorMessage
import org.strigate.ferrot.extensions.toSafeFileName
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.util.sha256
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

class DownloadWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val analyticsLogger: AnalyticsLogger,
    private val notificationService: NotificationService,
    private val settingsUseCase: SettingsUseCase,
    private val downloadPathProvider: DownloadPathProvider,
    private val youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase,
    private val downloadUseCase: DownloadUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    private var _downloadId: Long = -1L

    private var lastForegroundProgress: Int = -1
    private val qualityProfile: QualityProfile = QualityProfile.MAX

    override suspend fun doWork(): Result {
        _downloadId = inputData.getLong(KEY_ID, -1L)

        val downloadId = _downloadId
        val tag = "Download[$downloadId]:"
        Log.d(LOG_TAG, "$tag Start")

        if (runAttemptCount > 20 || downloadId <= 0L) {
            Log.w(LOG_TAG, "$tag Invalid download ID or attempts exhausted")
            return Result.failure()
        }

        val download = downloadUseCase.getDownloadByIdUseCase(downloadId)
            ?: return handleDownloadFailedResult()

        val notificationExtras = downloadNotificationExtras(download.id)
        enableDownloadForeground(
            downloadId = downloadId,
            notificationText = appContext.getString(R.string.notification_text_downloading),
            indeterminate = true,
            contentText = download.url,
            extras = notificationExtras,
        )

        var wasDownloadDeleted = false
        return coroutineScope mainScope@{
            try {
                var thumbnailFilePath: String? = null
                suspend fun throwIfDownloadDeleted() {
                    if (downloadUseCase.getDownloadByIdUseCase(downloadId) != null) {
                        return
                    }
                    Log.w(LOG_TAG, "$tag Download record no longer exists")
                    wasDownloadDeleted = true
                    throw CancellationException()
                }

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
                    Log.w(LOG_TAG, "$tag Cannot start from status=${download.status}")
                    return@mainScope Result.failure()
                }
                analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_STARTED)

                resetProgressAndCleanup()
                downloadUseCase.updateDownloadErrorMessageUseCase(downloadId, null)
                downloadUseCase.updateDownloadStartedAtUseCase(
                    downloadId = downloadId,
                    startedAtMillis = System.currentTimeMillis(),
                )

                val uidDir = downloadPathProvider.uidDir(download.uid)
                if (!uidDir.exists() && !uidDir.mkdirs()) {
                    return@mainScope Result.failure()
                }

                Log.d(LOG_TAG, "$tag Metadata: fetching")
                downloadUseCase.updateDownloadStatusUseCase(downloadId, DownloadStatus.METADATA)
                val videoInfo = withContext(Dispatchers.IO) {
                    runCatching {
                        youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
                    }.getOrNull()
                }
                val metadataMessage = if (videoInfo != null) {
                    "$tag Metadata: fetched"
                } else {
                    "$tag Metadata: unavailable"
                }
                Log.d(LOG_TAG, metadataMessage)

                val videoInfoTitle = videoInfo?.title?.takeIf { it.isNotBlank() }
                val videoInfoExtension = videoInfo?.ext?.takeIf { it.isNotBlank() }
                val videoInfoDuration = videoInfo?.duration ?: -1
                val videoInfoVideoBytes = videoInfo
                    ?.fileSize
                    ?.takeIf { it > 0L }
                    ?: videoInfo
                        ?.fileSizeApproximate
                        ?.takeIf { it > 0L }

                val videoTitle = videoInfoTitle ?: "Download_$downloadId"
                updateDownloadForeground(
                    notificationText = appContext.getString(R.string.notification_text_downloading),
                    indeterminate = true,
                    contentText = videoTitle,
                    extras = notificationExtras,
                )

                Log.d(LOG_TAG, "$tag Thumbnail: fetching")
                if (videoInfo?.id != null) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            thumbnailFilePath = youtubeDlAndroidUseCase
                                .downloadThumbnailUseCase(
                                    url = download.url,
                                    outputDir = uidDir,
                                    videoId = videoInfo.id,
                                )
                            throwIfDownloadDeleted()
                            downloadMetadataUseCase.saveDownloadMetadataUseCase(
                                DownloadMetadata(
                                    downloadId = downloadId,
                                    videoId = videoInfo.id,
                                    source = videoInfo.extractorKey?.lowercase(),
                                    title = videoInfo.title,
                                    thumbnailFilePath = thumbnailFilePath,
                                    durationSeconds = videoInfo.duration.takeIf { it > 0 },
                                )
                            )
                            val thumbnailMessage = if (thumbnailFilePath != null) {
                                "$tag Thumbnail: ready"
                            } else {
                                "$tag Thumbnail: unavailable"
                            }
                            Log.d(LOG_TAG, thumbnailMessage)
                        }.onFailure {
                            Log.w(LOG_TAG, "$tag Thumbnail: failed", it)
                        }
                    }
                }

                if (videoInfoVideoBytes != null) {
                    downloadProgressUseCase.updateDownloadExpectedBytesUseCase(
                        id = downloadId,
                        expectedBytes = videoInfoVideoBytes,
                    )
                }
                throwIfDownloadDeleted()
                downloadUseCase.updateDownloadStatusUseCase(
                    downloadId = downloadId,
                    status = DownloadStatus.DOWNLOADING,
                )

                val weights = decideWeights(
                    videoBytes = videoInfoVideoBytes,
                    audioBytes = audioByteEstimateFromDuration(videoInfoDuration),
                )

                val baseProcessId = "dl-$downloadId-${System.nanoTime()}"
                val videoProcessId = "${baseProcessId}_video"
                val audioProcessId = "${baseProcessId}_audio"

                val message = buildString {
                    append("$tag Prepared phases: ")
                    append("videoWeight=${weights.video} audioWeight=${weights.audio}")
                }
                Log.d(LOG_TAG, message)

                var maxBytes = 0L
                val bytesProviderRaw = {
                    maxBytes = max(maxBytes, directoryBytesSum(uidDir))
                    maxBytes
                }
                val title = videoTitle.toSafeFileName()
                val videoTemplate = "${uidDir.absolutePath}/${title} [%(id)s] - Video.%(ext)s"
                val audioTemplate = "${uidDir.absolutePath}/${title} [%(id)s] - Audio.%(ext)s"

                val phaseContext = PhaseContext(
                    phase = DownloadMediaType.VIDEO,
                    weights = weights,
                    title = videoTitle,
                    notificationExtras = notificationExtras,
                )
                val videoOutputPathFile = File(uidDir, ".video-output-path.txt")

                Log.d(LOG_TAG, "$tag Video: downloading")
                val videoOutputFilePath = withContext(Dispatchers.IO) {
                    collectPhase(
                        processId = videoProcessId,
                        url = download.url,
                        template = videoTemplate,
                        qualityProfile = qualityProfile,
                        outputPathFile = videoOutputPathFile,
                        bytesProviderRaw = bytesProviderRaw,
                        phaseContext = phaseContext.copy(
                            phase = DownloadMediaType.VIDEO,
                        ),
                        initialVideoPercent = 0f,
                        onCanceled = {
                            wasDownloadDeleted = true
                        },
                        onCombined = {},
                    )
                }
                Log.d(LOG_TAG, "$tag Video: downloaded")

                throwIfDownloadDeleted()

                if (videoOutputFilePath.isNullOrBlank()) {
                    Log.w(LOG_TAG, "$tag Video: output path not reported")
                    return@mainScope handleDownloadFailedResult()
                }
                val videoOutputFile = File(videoOutputFilePath)
                if (!videoOutputFile.exists() || videoOutputFile.length() <= 0L) {
                    Log.w(LOG_TAG, "$tag Video: output file missing or empty")
                    return@mainScope handleDownloadFailedResult()
                }

                val videoOutputFileExtension = videoInfoExtension ?: videoOutputFilePath
                    .extractFileExtension()
                    .orEmpty()

                val sha256 = withContext(Dispatchers.IO) {
                    sha256(videoOutputFilePath)
                }
                downloadVideoUseCase.saveDownloadVideoUseCase(
                    DownloadVideo(
                        downloadId = downloadId,
                        filePath = videoOutputFilePath,
                        fileExtension = videoOutputFileExtension,
                        sha256 = sha256,
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

                Log.d(LOG_TAG, "$tag Audio: downloading")
                try {
                    val audioOutputPathFile = File(uidDir, ".audio-output-path.txt")
                    val audioOutputFilePath = withContext(Dispatchers.IO) {
                        collectPhase(
                            processId = audioProcessId,
                            url = download.url,
                            template = audioTemplate,
                            qualityProfile = qualityProfile,
                            outputPathFile = audioOutputPathFile,
                            bytesProviderRaw = bytesProviderRaw,
                            phaseContext = phaseContext.copy(
                                phase = DownloadMediaType.AUDIO,
                            ),
                            initialVideoPercent = 100f,
                            onCanceled = {
                                wasDownloadDeleted = true
                            },
                            onCombined = {},
                        )
                    }
                    Log.d(LOG_TAG, "$tag Audio: downloaded")

                    if (audioOutputFilePath.isNullOrBlank()) {
                        throw IllegalStateException("Audio output file path was not reported")
                    }
                    val audioOutputFile = File(audioOutputFilePath)
                    if (!audioOutputFile.exists() || audioOutputFile.length() <= 0L) {
                        throw IllegalStateException("Audio output file path was reported but file is missing")
                    }

                    val audioOutputFileExtension = audioOutputFilePath
                        .extractFileExtension()
                        .orEmpty()

                    downloadAudioUseCase.saveDownloadAudioUseCase(
                        DownloadAudio(
                            downloadId = downloadId,
                            filePath = audioOutputFilePath,
                            fileExtension = audioOutputFileExtension,
                        )
                    )
                } catch (throwable: Throwable) {
                    val message = "$tag Audio: failed, continuing with video only"
                    Log.w(LOG_TAG, message, throwable)
                }

                downloadUseCase.updateDownloadErrorMessageUseCase(downloadId, null)
                downloadUseCase.updateDownloadStatusUseCase(
                    status = DownloadStatus.COMPLETED,
                    downloadId = downloadId,
                )
                downloadUseCase.updateDownloadCompletedAtUseCase(
                    completedAtMillis = System.currentTimeMillis(),
                    downloadId = downloadId,
                )
                analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_COMPLETED)

                val automaticDuplicateDownloadDeletionSetting = settingsUseCase
                    .getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase()
                    .first()
                if (automaticDuplicateDownloadDeletionSetting) {
                    DeleteAllDuplicateDownloadsWorker.enqueueDebouncedReplace(appContext)
                }
                DeleteAllOrphanDownloadFilesWorker.enqueueDebouncedReplace(appContext)

                val downloadComplete = appContext.getString(R.string.download_complete)
                Log.d(LOG_TAG, "$tag Complete")
                appContext.toast("$downloadComplete: $videoTitle", true)

                notificationService.notifyDownloaded(
                    contentText = videoTitle,
                    contentTitle = downloadComplete,
                    extras = notificationExtras,
                    tag = downloadNotificationTag(downloadId),
                    actions = buildCompletedNotificationActions(
                        downloadId = downloadId,
                        shareFilePath = videoOutputFilePath,
                    ),
                    thumbnailFilePath = thumbnailFilePath,
                    autoCancel = false,
                )
                Result.success()

            } catch (throwable: Throwable) {
                Log.w(LOG_TAG, "$tag Failed", throwable)

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

                Log.w(LOG_TAG, "$tag Cancelled: stopReason=$stopReason")
                return@mainScope when (stopReason) {
                    WorkInfo.STOP_REASON_CANCELLED_BY_APP,
                    WorkInfo.STOP_REASON_USER -> {
                        Log.w(LOG_TAG, "$tag Cancelled by app or user")
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
                    downloadId = downloadId,
                )
            }
        }
        val downloadFailed = appContext.getString(R.string.download_failed)
        appContext.toast(downloadFailed, true)
        notificationService.notifyDownloaded(
            contentTitle = downloadFailed,
            contentText = notificationText,
            extras = notificationExtras,
            tag = downloadNotificationTag(downloadId),
            actions = buildFailedNotificationActions(downloadId),
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
                        downloadUseCase.updateDownloadStatusUseCase(
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
                    downloadUseCase.updateDownloadStatusUseCase(
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

    private suspend fun enableDownloadForeground(
        downloadId: Long,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        enableForeground(
            notificationId = downloadId.toInt(),
            notificationText = notificationText,
            progress = progress,
            indeterminate = indeterminate,
            contentText = contentText,
            extras = extras,
            actions = buildActiveNotificationActions(),
        )
    }

    private fun updateDownloadForeground(
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        updateForeground(
            notificationText = notificationText,
            progress = progress,
            indeterminate = indeterminate,
            contentText = contentText,
            extras = extras,
            actions = buildActiveNotificationActions(),
        )
    }

    private fun buildActiveNotificationActions(): List<NotificationCompat.Action> {
        return listOf(
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = _downloadId,
                actionType = DownloadNotificationActionType.STOP,
            ),
        )
    }

    private fun buildCompletedNotificationActions(
        downloadId: Long,
        shareFilePath: String?,
    ): List<NotificationCompat.Action> {
        val actions = mutableListOf<NotificationCompat.Action>()
        shareFilePath?.let { filePath ->
            val shareFile = File(filePath)
            if (shareFile.exists() && shareFile.length() > 0L) {
                actions += buildShareDownloadNotificationAction(
                    context = appContext,
                    downloadId = downloadId,
                    filePath = filePath,
                )
            }
        }
        actions += buildDownloadNotificationAction(
            context = appContext,
            downloadId = downloadId,
            actionType = DownloadNotificationActionType.MARK_SEEN,
        )
        actions += buildDownloadNotificationAction(
            context = appContext,
            downloadId = downloadId,
            actionType = DownloadNotificationActionType.DELETE,
        )
        return actions
    }

    private fun buildFailedNotificationActions(
        downloadId: Long,
    ): List<NotificationCompat.Action> {
        return listOf(
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = downloadId,
                actionType = DownloadNotificationActionType.RETRY,
            ),
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = downloadId,
                actionType = DownloadNotificationActionType.DELETE,
            ),
        )
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
        outputPathFile: File,
        bytesProviderRaw: () -> Long,
        phaseContext: PhaseContext,
        initialVideoPercent: Float,
        onCanceled: () -> Unit,
        onCombined: (Float) -> Unit,
    ): String? {
        val throttle = ProgressThrottle()
        var outputFilePath: String? = null

        val downloadTickFlow = youtubeDlAndroidUseCase.downloadWithProgressUseCase(
            url = url,
            template = template,
            profile = qualityProfile,
            processId = processId,
            bytesProvider = { throttle.throttledBytes(bytesProviderRaw) },
            downloadMediaType = phaseContext.phase,
            outputPathFile = outputPathFile,
            onOutputFilePath = { outputFilePath = it },
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
                    updateDownloadForeground(
                        notificationText = appContext.getString(R.string.notification_text_downloading),
                        progress = percentInt,
                        indeterminate = false,
                        contentText = contentLine,
                        extras = phaseContext.notificationExtras,
                    )
                    lastForegroundProgress = percentInt
                }
            }
            return outputFilePath
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
