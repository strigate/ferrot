package org.strigate.ferrot.domain.usecase.youtubedl_android

import android.os.SystemClock
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import org.strigate.ferrot.app.integration.YoutubeDlClient
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.download.MetadataStripException
import org.strigate.ferrot.domain.usecase.download.StripMediaMetadataUseCase
import org.strigate.ferrot.domain.usecase.settings.GetIncludeAttributionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetStripMediaMetadataEnabledSettingAsFlowUseCase
import java.io.File
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class DownloadWithProgressUseCase @Inject constructor(
    private val buildVideoDownloadRequestUseCase: BuildVideoDownloadRequestUseCase,
    private val buildAudioDownloadRequestUseCase: BuildAudioDownloadRequestUseCase,
    private val youtubeDlRuntimeInitializer: YoutubeDlRuntimeInitializer,
    private val youtubeDlClient: YoutubeDlClient,
    private val getIncludeAttributionEnabledSettingAsFlowUseCase: GetIncludeAttributionEnabledSettingAsFlowUseCase,
    private val getStripMediaMetadataEnabledSettingAsFlowUseCase: GetStripMediaMetadataEnabledSettingAsFlowUseCase,
    private val stripMediaMetadataUseCase: StripMediaMetadataUseCase,
) {
    operator fun invoke(
        url: String,
        template: String,
        profile: QualityProfile,
        processId: String,
        bytesProvider: () -> Long,
        downloadMediaType: DownloadMediaType = DownloadMediaType.VIDEO,
        outputPathFile: File? = null,
        onOutputFilePath: ((String) -> Unit)? = null,
        cookieFilePath: String? = null,
    ) = callbackFlow {
        val progressMappingPolicy = ProgressMappingPolicy()
        val job = launch {
            youtubeDlRuntimeInitializer.initializeIfNeeded()
            outputPathFile?.delete()
            val stripMediaMetadataEnabled = getStripMediaMetadataEnabledSettingAsFlowUseCase()
                .first()

            val youtubeDlRequest: YoutubeDLRequest = when (downloadMediaType) {
                DownloadMediaType.VIDEO -> {
                    buildVideoDownloadRequestUseCase(
                        url = url,
                        template = template,
                        qualityProfile = profile,
                        includeAttributionEnabled = getIncludeAttributionEnabledSettingAsFlowUseCase().first(),
                        stripMediaMetadataEnabled = stripMediaMetadataEnabled,
                        noProgress = false,
                        outputPathFilePath = outputPathFile?.absolutePath,
                        cookieFilePath = cookieFilePath,
                    )
                }

                DownloadMediaType.AUDIO -> {
                    buildAudioDownloadRequestUseCase(
                        url = url,
                        template = template,
                        noProgress = false,
                        outputPathFilePath = outputPathFile?.absolutePath,
                        cookieFilePath = cookieFilePath,
                    )
                }
            }

            val youtubeDlResponse = youtubeDlClient.execute(
                request = youtubeDlRequest,
                processId = processId,
                redirectErrorStream = false,
            ) { rawPercent, rawEta, _ ->
                val mapped = progressMappingPolicy.map(rawPercent) ?: return@execute
                trySend(
                    DownloadTick(
                        percent = if (stripMediaMetadataEnabled) mapped.coerceAtMost(99f) else mapped,
                        etaSeconds = rawEta.takeIf { it >= 0 },
                        bytesDownloaded = bytesProvider(),
                    ),
                )
            }
            if (youtubeDlResponse.exitCode != 0) {
                throw IllegalStateException("Exit code ${youtubeDlResponse.exitCode}")
            }
            val outputPath = outputPathFile?.let(::readAfterMoveOutputFilePath)
            if (stripMediaMetadataEnabled) {
                if (outputPath == null) throw MetadataStripException()
                trySend(
                    DownloadTick(
                        percent = 99f,
                        etaSeconds = null,
                        bytesDownloaded = bytesProvider()
                    )
                )
                stripMediaMetadataUseCase(File(outputPath))
            }
            outputPath?.let { onOutputFilePath?.invoke(it) }
            close()
        }
        awaitClose {
            outputPathFile?.delete()
            runCatching {
                youtubeDlClient.destroyProcessById(processId)
            }
            job.cancel()
        }
    }

    private class ProgressMappingPolicy(
        private val stageWeights: FloatArray = floatArrayOf(0.88f, 0.10f, 0.02f),
        private val resetThresholdPercent: Float = 5f,
        private val minUpdateIntervalMillis: Long = 150,
        private val minProgressDeltaPercent: Float = 0.5f,
    ) {
        private var stageIndex = 0
        private var stageMaxPercent = 0f
        private var lastReportedPercent = 0f
        private var lastUpdateTimeMillis = 0L

        fun map(rawPercent: Float): Float? {
            val now = SystemClock.uptimeMillis()
            val clampedPercent = rawPercent.coerceIn(0f, 100f)
            if (clampedPercent + resetThresholdPercent < stageMaxPercent) {
                stageIndex += 1
                stageMaxPercent = 0f
            }
            if (clampedPercent > stageMaxPercent) {
                stageMaxPercent = clampedPercent
            }
            val completedWeight = stageWeights.take(stageIndex).sum().coerceIn(0f, 1f)
            val currentWeight = if (stageIndex < stageWeights.size) {
                stageWeights[stageIndex]
            } else {
                (1f - completedWeight).coerceAtLeast(0f)
            }
            val weightedProgress =
                ((completedWeight * 100f) + (currentWeight * (stageMaxPercent / 100f) * 100f))
                    .coerceIn(0f, 100f)

            val stableProgress = max(weightedProgress, lastReportedPercent)
            if (now - lastUpdateTimeMillis < minUpdateIntervalMillis) {
                return null
            }
            if (abs(stableProgress - lastReportedPercent) < minProgressDeltaPercent) {
                return null
            }
            lastReportedPercent = stableProgress
            lastUpdateTimeMillis = now
            return stableProgress
        }
    }

    data class DownloadTick(
        val percent: Float,
        val etaSeconds: Long?,
        val bytesDownloaded: Long,
    )
}

private fun readAfterMoveOutputFilePath(file: File): String? {
    if (!file.exists()) {
        return null
    }
    return runCatching {
        file.readLines()
            .asReversed()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
