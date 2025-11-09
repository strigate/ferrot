package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.presentation.model.DownloadUiData

fun Download.toUiData(
    metadata: DownloadMetadata?,
    progress: DownloadProgress?,
): DownloadUiData {
    val videoFilePathNonNull = videoFilePath.orEmpty()
    val title = metadata?.title ?: videoFilePathNonNull
        .substringBeforeLast('.', missingDelimiterValue = videoFilePathNonNull)
        .ifBlank { url }
    val videoFileName = videoFilePathNonNull
        .substringAfterLast("/", missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }

    val audioFilePathNonNull = audioFilePath.orEmpty()
    val audioFileName = audioFilePathNonNull
        .substringAfterLast("/", missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }

    val fraction = progress?.progressPercent
        ?.let { it.coerceIn(0f, 100f) / 100f }
        ?.takeIf { it.isFinite() }

    return DownloadUiData(
        title = title,
        url = url,
        videoFilePath = videoFilePath,
        videoFileName = videoFileName,
        audioFilePath = audioFilePath,
        audioFileName = audioFileName,
        status = status.toUiData(),
        errorMessage = errorMessage,
        progressFraction = fraction,
        bytesDownloaded = progress?.bytesDownloaded ?: 0L,
        etaSeconds = progress?.etaSeconds,
        expectedBytes = progress?.expectedBytes,
        thumbnailFilePath = metadata?.thumbnailFilePath,
    )
}
