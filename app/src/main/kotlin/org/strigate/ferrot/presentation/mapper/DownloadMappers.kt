package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.presentation.model.DownloadAudioUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.model.DownloadVideoUiData
import java.util.Locale

fun Download.toUiData(
    video: DownloadVideo?,
    audio: DownloadAudio?,
    metadata: DownloadMetadata?,
    progress: DownloadProgress?,
): DownloadUiData {
    val downloadVideoUiData = video?.filePath
        ?.takeIf { it.isNotBlank() }
        ?.let { filePath ->
            DownloadVideoUiData(
                filePath = filePath,
                fileName = filePath.extractFileName(),
                extension = filePath.extractFileExtension(),
            )
        }

    val downloadAudioUiData = audio?.filePath
        ?.takeIf { it.isNotBlank() }
        ?.let { filePath ->
            DownloadAudioUiData(
                filePath = filePath,
                fileName = filePath.extractFileName(),
                extension = filePath.extractFileExtension(),
            )
        }

    val derivedTitleFromVideo = downloadVideoUiData?.fileName?.stripFileExtension()
        ?.takeIf { it.isNotBlank() }
    val derivedTitleFromAudio = downloadAudioUiData?.fileName?.stripFileExtension()
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

private fun String.extractFileName() = substringAfterLast('/', missingDelimiterValue = "")
    .takeIf { it.isNotBlank() }

private fun String.stripFileExtension() = substringBeforeLast('.', missingDelimiterValue = this)

private fun String.extractFileExtension() = extractFileName()
    ?.substringAfterLast('.', missingDelimiterValue = "")
    ?.takeIf { it.isNotBlank() }
    ?.uppercase(Locale.ROOT)
