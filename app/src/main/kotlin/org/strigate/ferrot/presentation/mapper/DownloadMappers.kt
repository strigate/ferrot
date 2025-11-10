package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.presentation.model.DownloadAudioUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.model.DownloadVideoUiData

fun Download.toUiData(
    video: DownloadVideo?,
    audio: DownloadAudio?,
    metadata: DownloadMetadata?,
    progress: DownloadProgress?,
): DownloadUiData {
    val downloadVideoUiData = video?.filePath
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            DownloadVideoUiData(
                filePath = path,
                fileName = path
                    .substringAfterLast("/", missingDelimiterValue = "")
                    .takeIf { it.isNotBlank() },
            )
        }
    val downloadAudioUiData = audio?.filePath
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            DownloadAudioUiData(
                filePath = path,
                fileName = path
                    .substringAfterLast("/", missingDelimiterValue = "")
                    .takeIf { it.isNotBlank() },
            )
        }

    val derivedTitleFromVideo = downloadVideoUiData?.fileName
        ?.substringBeforeLast('.', missingDelimiterValue = downloadVideoUiData.fileName)
        ?.takeIf { it.isNotBlank() }
    val derivedTitleFromAudio = downloadAudioUiData?.fileName
        ?.substringBeforeLast('.', missingDelimiterValue = downloadAudioUiData.fileName)
        ?.takeIf { it.isNotBlank() }
    val titleValue = metadata?.title
        ?.takeIf { it.isNotBlank() }
        ?: derivedTitleFromVideo
        ?: derivedTitleFromAudio
        ?: url
    val fraction = progress?.progressPercent
        ?.let { it.coerceIn(0f, 100f) / 100f }
        ?.takeIf { it.isFinite() }

    return DownloadUiData(
        title = titleValue,
        url = url,
        status = status.toUiData(),
        video = downloadVideoUiData,
        audio = downloadAudioUiData,
        errorMessage = errorMessage,
        progressFraction = fraction,
        bytesDownloaded = progress?.bytesDownloaded ?: 0L,
        etaSeconds = progress?.etaSeconds,
        expectedBytes = progress?.expectedBytes,
        thumbnailFilePath = metadata?.thumbnailFilePath,
    )
}
