package org.strigate.ferrot.domain.usecase.youtubedl_android

import android.os.SystemClock
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.youtubedl_android.internal.extractFinalOutputFilePath
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class DownloadWithProgressUseCase @Inject constructor(
    private val buildVideoDownloadRequestUseCase: BuildVideoDownloadRequestUseCase,
    private val buildAudioDownloadRequestUseCase: BuildAudioDownloadRequestUseCase,
) {
    operator fun invoke(
        url: String,
        template: String,
        profile: QualityProfile,
        processId: String,
        bytesProvider: () -> Long,
        downloadMediaType: DownloadMediaType = DownloadMediaType.VIDEO,
    ) = callbackFlow {
        val progressMappingPolicy = ProgressMappingPolicy()
        val job = launch {
            val youtubeDlRequest: YoutubeDLRequest = when (downloadMediaType) {
                DownloadMediaType.VIDEO -> {
                    buildVideoDownloadRequestUseCase(
                        url = url,
                        template = template,
                        qualityProfile = profile,
                        noProgress = false,
                    )
                }

                DownloadMediaType.AUDIO -> {
                    buildAudioDownloadRequestUseCase(
                        url = url,
                        template = template,
                        noProgress = false,
                    )
                }
            }

            val youtubeDlResponse = YoutubeDL.getInstance().execute(
                request = youtubeDlRequest,
                processId = processId,
                redirectErrorStream = false,
            ) { rawPercent, rawEta, _ ->
                val mapped = progressMappingPolicy.map(rawPercent) ?: return@execute
                trySend(
                    DownloadTick(
                        percent = mapped,
                        etaSeconds = rawEta.takeIf { it >= 0 },
                        bytesDownloaded = bytesProvider(),
                    ),
                )
            }
            val outputFilePath = extractFinalOutputFilePath(youtubeDlResponse.out)
            if (!outputFilePath.isNullOrBlank()) {
                trySend(
                    DownloadTick(
                        percent = 100f,
                        etaSeconds = null,
                        bytesDownloaded = bytesProvider(),
                        outputFilePath = outputFilePath,
                    ),
                )
            }
            if (youtubeDlResponse.exitCode != 0) {
                throw IllegalStateException("Exit code ${youtubeDlResponse.exitCode}")
            }
            close()
        }
        awaitClose {
            runCatching {
                YoutubeDL.getInstance().destroyProcessById(processId)
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
        val outputFilePath: String? = null,
    )
}
