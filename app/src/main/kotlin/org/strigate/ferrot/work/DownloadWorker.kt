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
import org.strigate.ferrot.app.Constants.Notifications.Ids.ID_ACTIVE_DOWNLOAD_FOREGROUND
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.Constants.Work.Name.KEY_WIFI_ONLY
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DOWNLOAD
import org.strigate.ferrot.app.DownloadNotificationActionType
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.activeDownloadNotificationTag
import org.strigate.ferrot.app.buildDownloadNotificationAction
import org.strigate.ferrot.app.downloadNotificationExtras
import org.strigate.ferrot.app.downloadNotificationTag
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
                    downloadId = downloadId,
                    startedAtMillis = System.currentTimeMillis(),
                )

                val uidDir = downloadPathProvider.uidDir(download.uid)
                if (!uidDir.exists() && !uidDir.mkdirs()) {
                    return@mainScope Result.failure()
                }

                Log.d(LOG_TAG, "$tag Downloading metadata")
                downloadUseCase.updateDownloadStatusUseCase(downloadId, DownloadStatus.METADATA)
                val videoInfo = withContext(Dispatchers.IO) {
                    runCatching {
                        youtubeDlAndroidUseCase.getVideoInfoUseCase(download.url)
                    }.getOrNull()
                }
                val message = if (videoInfo != null) {
                    "Downloaded metadata"
                } else {
                    "Unable to download metadata"
                }
                Log.d(LOG_TAG, "$tag $message")

                val videoInfoId = videoInfo?.id
                val videoInfoTitle = videoInfo?.title?.takeIf { it.isNotBlank() }
                val videoInfoExtension = videoInfo?.ext?.takeIf { it.isNotBlank() }
                val videoInfoDuration = videoInfo?.duration ?: -1
                val videoInfoVideoBytes = videoInfo
                    ?.fileSize
                    ?.takeIf { it > 0L }
                    ?: videoInfo
                        ?.fileSizeApproximate
                        ?.takeIf { it > 0L }

                videoTitle = videoInfoTitle ?: "Download_$downloadId"

                updateDownloadForeground(
                    notificationText = appContext.getString(R.string.notification_text_downloading),
                    indeterminate = true,
                    contentText = videoTitle,
                    extras = notificationExtras,
                )

                Log.d(LOG_TAG, "$tag Downloading thumbnail")
                if (videoInfo?.id != null) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val thumbnailFilePath = youtubeDlAndroidUseCase
                                .downloadThumbnailUseCase(
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
                                    videoId = videoInfo.id,
                                    source = videoInfo.extractorKey?.lowercase(),
                                    title = videoInfo.title,
                                    thumbnailFilePath = thumbnailFilePath,
                                    durationSeconds = videoInfo.duration.takeIf { it > 0 },
                                )
                            )
                            Log.d(LOG_TAG, "$tag Downloaded thumbnail")
                        }.onFailure {
                            Log.w(LOG_TAG, "$tag Unable to download thumbnail", it)
                        }
                    }
                }

                if (videoInfoVideoBytes != null) {
                    downloadProgressUseCase.updateDownloadExpectedBytesUseCase(
                        id = downloadId,
                        expectedBytes = videoInfoVideoBytes,
                    )
                }
                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    wasDownloadDeleted = true
                    throw CancellationException()
                }
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

                Log.d(LOG_TAG, "$tag Decided weights: v=${weights.video}, a=${weights.audio}")
                Log.d(LOG_TAG, "$tag videoProcessId: $videoProcessId")
                Log.d(LOG_TAG, "$tag audioProcessId: $audioProcessId")

                var maxBytes = 0L
                val bytesProviderRaw = {
                    maxBytes = max(maxBytes, directoryBytesSum(uidDir))
                    maxBytes
                }
                val title = (videoInfoTitle ?: videoTitle ?: "Download").toSafeFileName()
                val videoTemplate = "${uidDir.absolutePath}/${title} [%(id)s] - Video.%(ext)s"
                val audioTemplate = "${uidDir.absolutePath}/${title} [%(id)s] - Audio.%(ext)s"

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
                Log.d(LOG_TAG, "$tag Downloaded video")

                if (downloadUseCase.getDownloadByIdUseCase(downloadId) == null) {
                    Log.w(LOG_TAG, "$tag Download record was deleted during download")
                    wasDownloadDeleted = true
                    throw CancellationException()
                }

                val videoOutputFile = locateOutputFileByInfoId(uidDir, videoInfoId)
                if (videoOutputFile == null || !videoOutputFile.exists()) {
                    Log.w(LOG_TAG, "$tag Video output file could not be located or does not exist")
                    return@mainScope handleDownloadFailedResult()
                }

                val videoOutputFilePath = videoOutputFile.absolutePath
                val videoOutputFileExtension = videoInfoExtension ?: videoOutputFilePath
                    .extractFileExtension()
                    .orEmpty()

                Log.d(LOG_TAG, "$tag Video output file exists, calculating hash")
                val sha256 = withContext(Dispatchers.IO) {
                    sha256(videoOutputFilePath)
                }

                Log.d(LOG_TAG, "$tag Calculated video file hash, saving download video")
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

                Log.d(LOG_TAG, "$tag Downloading audio")
                try {
                    withContext(Dispatchers.IO) {
                        collectPhase(
                            processId = audioProcessId,
                            url = download.url,
                            template = audioTemplate,
                            qualityProfile = qualityProfile,
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
                    Log.d(LOG_TAG, "$tag Downloaded audio")
                } catch (throwable: Throwable) {
                    val message = "$tag Audio download failed, continuing with video-only"
                    Log.w(LOG_TAG, message, throwable)
                }

                val audioOutputFile = locateOutputFileByInfoId(
                    dir = uidDir,
                    videoInfoId = videoInfoId,
                    audio = true,
                )
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
                updateDownloadForeground(
                    notificationText = appContext.getString(R.string.download_complete),
                    progress = finalPercent,
                    indeterminate = false,
                    contentText = videoTitle ?: download.url,
                    extras = notificationExtras,
                )

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
                val contentText = videoTitle ?: download.url

                Log.d(LOG_TAG, "$tag $downloadComplete")
                appContext.toast("$downloadComplete: $contentText", true)
                clearActiveDownloadNotification(downloadId)
                notificationService.notifyDownloaded(
                    contentText = contentText,
                    contentTitle = downloadComplete,
                    extras = notificationExtras,
                    tag = downloadNotificationTag(downloadId),
                    actions = buildCompletedNotificationActions(downloadId),
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
                    downloadId = downloadId,
                )
            }
        }
        val downloadFailed = appContext.getString(R.string.download_failed)
        appContext.toast(downloadFailed, true)
        clearActiveDownloadNotification(downloadId)
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
                    clearActiveDownloadNotification(downloadId)
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
                clearActiveDownloadNotification(downloadId)
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
        clearActiveDownloadNotification(downloadId)
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
        syncActiveDownloadNotification(
            downloadId = downloadId,
            notificationText = notificationText,
            progress = progress,
            indeterminate = indeterminate,
            contentText = contentText,
            extras = extras,
        )
        enableForeground(
            notificationId = ID_ACTIVE_DOWNLOAD_FOREGROUND.toLong(),
            notificationText = appContext.getString(R.string.notification_title_downloads_in_progress),
            actions = buildForegroundNotificationActions(),
        )
    }

    private fun updateDownloadForeground(
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        syncActiveDownloadNotification(
            downloadId = _downloadId,
            notificationText = notificationText,
            progress = progress,
            indeterminate = indeterminate,
            contentText = contentText,
            extras = extras,
        )
        updateForeground(
            notificationText = appContext.getString(R.string.notification_title_downloads_in_progress),
            actions = buildForegroundNotificationActions(),
        )
    }

    private fun syncActiveDownloadNotification(
        downloadId: Long,
        notificationText: String,
        progress: Int? = null,
        indeterminate: Boolean = false,
        contentText: String? = null,
        extras: Map<String, String>? = null,
    ) {
        if (downloadId <= 0L) {
            return
        }
        notificationService.notifyActiveDownload(
            contentTitle = notificationText,
            contentText = contentText.orEmpty(),
            extras = extras.orEmpty(),
            tag = activeDownloadNotificationTag(downloadId),
            actions = buildActiveNotificationActions(),
            notificationId = downloadId.toInt(),
            progress = progress,
            indeterminate = indeterminate,
        )
    }

    private fun clearActiveDownloadNotification(downloadId: Long = _downloadId) {
        if (downloadId <= 0L) {
            return
        }
        notificationService.clearNotification(
            notificationId = downloadId.toInt(),
            tag = activeDownloadNotificationTag(downloadId),
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

    private fun buildForegroundNotificationActions(): List<NotificationCompat.Action> {
        return listOf(
            buildDownloadNotificationAction(
                context = appContext,
                downloadId = -1L,
                actionType = DownloadNotificationActionType.STOP_ALL,
            ),
        )
    }

    private fun buildCompletedNotificationActions(downloadId: Long): List<NotificationCompat.Action> {
        val actions = mutableListOf<NotificationCompat.Action>()
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

    private fun locateOutputFileByInfoId(
        dir: File,
        videoInfoId: String?,
        audio: Boolean = false,
    ): File? {
        if (!dir.exists()) {
            return null
        }
        val extensions = if (audio) {
            listOf("mp3", "m4a", "opus")
        } else {
            listOf("mp4", "mkv", "webm")
        }
        if (!videoInfoId.isNullOrBlank()) {
            extensions
                .map { File(dir, "$videoInfoId.$it") }
                .firstOrNull { it.exists() && it.length() > 0L }
                ?.let { return it }
        }
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
