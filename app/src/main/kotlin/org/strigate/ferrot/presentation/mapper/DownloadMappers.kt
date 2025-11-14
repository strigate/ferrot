package org.strigate.ferrot.presentation.mapper

import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.extensions.extractFileExtension
import org.strigate.ferrot.extensions.extractFileName
import org.strigate.ferrot.extensions.stripFileExtension
import org.strigate.ferrot.presentation.model.DownloadAudioUiData
import org.strigate.ferrot.presentation.model.DownloadMetadataUiData
import org.strigate.ferrot.presentation.model.DownloadProgressUiData
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
        ?.let { filePath ->
            DownloadVideoUiData(
                filePath = filePath,
                fileName = filePath.extractFileName(),
                fileExtension = (
                        video.fileExtension.takeIf { it.isNotBlank() }
                            ?: filePath.extractFileExtension()
                        ).orEmpty(),
            )
        }

    val downloadAudioUiData = audio?.filePath
        ?.takeIf { it.isNotBlank() }
        ?.let { filePath ->
            DownloadAudioUiData(
                filePath = filePath,
                fileName = filePath.extractFileName(),
                fileExtension = (
                        audio.fileExtension.takeIf { it.isNotBlank() }
                            ?: filePath.extractFileExtension()
                        ).orEmpty(),
            )
        }

    val derivedTitleFromVideo = downloadVideoUiData
        ?.fileName
        ?.stripFileExtension()
        ?.takeIf { it.isNotBlank() }

    val derivedTitleFromAudio = downloadAudioUiData
        ?.fileName
        ?.stripFileExtension()
        ?.takeIf { it.isNotBlank() }

    val titleValue = metadata?.title
        ?.takeIf { it.isNotBlank() }
        ?: derivedTitleFromVideo
        ?: derivedTitleFromAudio
        ?: url

    val metadataUiData = DownloadMetadataUiData(
        title = titleValue,
        thumbnailFilePath = metadata?.thumbnailFilePath,
        durationSeconds = metadata?.durationSeconds,
    )

    val progressUiData = progress?.let {
        val fraction = it.progressPercent
            .coerceIn(0f, 100f)
            .div(100f)
            .takeIf { value -> value.isFinite() }
            ?: 0f

        DownloadProgressUiData(
            progressFraction = fraction,
            bytesDownloaded = it.bytesDownloaded,
            etaSeconds = it.etaSeconds,
            expectedBytes = it.expectedBytes,
        )
    }

    return DownloadUiData(
        url = url,
        status = status.toUiData(),
        metadata = metadataUiData,
        video = downloadVideoUiData,
        audio = downloadAudioUiData,
        progress = progressUiData,
        errorMessage = errorMessage,
    )
}
